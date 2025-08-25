package com.idsiber.eye;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

public class WebSocketService extends Service {
    private static final String TAG = "WebSocketService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "IdSiberEyeService";
    
    private WebSocketClient wsClient;
    private CommandHandler commandHandler;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        createNotificationChannel();
        commandHandler = new CommandHandler(this);
        
        // Initialize WebSocket client for service
        wsClient = new WebSocketClient(this, new WebSocketClient.StatusCallback() {
            @Override
            public void onStatusChange(String status) {
                updateNotification("Status: " + status);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "WebSocket error: " + error);
            }
        });
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        
        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification("Starting service..."));
        
        // Connect to server
        if (!wsClient.isConnected()) {
            wsClient.connect();
        }
        
        // Start heartbeat
        startHeartbeat();
        
        // Return sticky to restart if killed
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't provide binding
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        
        if (wsClient != null) {
            wsClient.disconnect();
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "IdSiber-Eye Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps connection to parent device active");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("IdSiber-Eye Active")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }
    
    private void updateNotification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, createNotification(text));
    }
    
    private void startHeartbeat() {
        // Send heartbeat every 30 seconds
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(30000); // 30 seconds
                        
                        if (wsClient != null && wsClient.isConnected()) {
                            wsClient.sendHeartbeat();
                            
                            // Also send status update
                            JSONObject status = getDeviceStatus();
                            wsClient.sendStatusUpdate(status);
                        }
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        Log.e(TAG, "Heartbeat error", e);
                    }
                }
            }
        }).start();
    }
    
    private JSONObject getDeviceStatus() {
        try {
            JSONObject status = new JSONObject();
            
            // Battery status
            Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryIntent != null) {
                int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                float batteryPct = level * 100 / (float) scale;
                boolean isCharging = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING;
                
                status.put("battery_level", (int) batteryPct);
                status.put("is_charging", isCharging);
            }
            
            // Device admin status
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            ComponentName adminReceiver = new ComponentName(this, IdSiberDeviceAdminReceiver.class);
            status.put("device_admin_active", dpm.isAdminActive(adminReceiver));
            
            // Screen status
            status.put("screen_on", isScreenOn());
            
            // Current foreground app
            status.put("current_app", getCurrentApp());
            
            status.put("timestamp", System.currentTimeMillis());
            
            return status;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating device status", e);
            return new JSONObject();
        }
    }
    
    private boolean isScreenOn() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS) > 0;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }
    
    private String getCurrentApp() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.RunningTaskInfo foregroundTaskInfo = am.getRunningTasks(1).get(0);
                return foregroundTaskInfo.topActivity.getPackageName();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting current app", e);
        }
        return "unknown";
    }
}