package com.idsiber.eye;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import android.os.PowerManager;

public class MainActivity extends Activity {
    
    private static final int REQUEST_ENABLE_ADMIN = 1;
    private static final int REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = 2;
    
    private WebSocketClient wsClient;
    private TextView statusText;
    private TextView deviceIdText;
    private Button connectBtn;
    private Button adminBtn;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminReceiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        initDeviceAdmin();
        initWebSocketClient();
        checkBatteryOptimization();
        
        updateUI();
    }
    
    private void initViews() {
        statusText = findViewById(R.id.status_text);
        deviceIdText = findViewById(R.id.device_id_text);
        connectBtn = findViewById(R.id.connect_btn);
        adminBtn = findViewById(R.id.admin_btn);
        
        // Set device ID
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        deviceIdText.setText("Device ID: " + deviceId);
        
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleConnection();
            }
        });
        
        adminBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestDeviceAdmin();
            }
        });
    }
    
    private void initDeviceAdmin() {
        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        adminReceiver = new ComponentName(this, IdSiberDeviceAdminReceiver.class);
    }
    
    private void initWebSocketClient() {
        wsClient = new WebSocketClient(this, new WebSocketClient.StatusCallback() {
            @Override
            public void onStatusChange(final String status) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText(status);
                        updateUI();
                    }
                });
            }
            
            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    
    private void toggleConnection() {
        if (wsClient.isConnected()) {
            wsClient.disconnect();
            stopWebSocketService();
        } else {
            wsClient.connect();
            startWebSocketService();
        }
    }
    
    private void startWebSocketService() {
        Intent serviceIntent = new Intent(this, WebSocketService.class);
        startForegroundService(serviceIntent);
    }
    
    private void stopWebSocketService() {
        Intent serviceIntent = new Intent(this, WebSocketService.class);
        stopService(serviceIntent);
    }
    
    private void requestDeviceAdmin() {
        if (!devicePolicyManager.isAdminActive(adminReceiver)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminReceiver);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                "This app needs device admin permission to lock the screen remotely.");
            startActivityForResult(intent, REQUEST_ENABLE_ADMIN);
        } else {
            Toast.makeText(this, "Device admin already enabled", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void checkBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        }
    }
    
    private void updateUI() {
        boolean isConnected = wsClient.isConnected();
        boolean isAdmin = devicePolicyManager.isAdminActive(adminReceiver);
        
        connectBtn.setText(isConnected ? "Disconnect" : "Connect");
        connectBtn.setEnabled(true);
        
        adminBtn.setText(isAdmin ? "Admin Enabled ✓" : "Enable Device Admin");
        adminBtn.setEnabled(!isAdmin);
        
        if (isConnected && isAdmin) {
            statusText.setTextColor(getColor(android.R.color.holo_green_dark));
        } else if (isConnected) {
            statusText.setTextColor(getColor(android.R.color.holo_orange_dark));
        } else {
            statusText.setTextColor(getColor(android.R.color.holo_red_dark));
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        switch (requestCode) {
            case REQUEST_ENABLE_ADMIN:
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "Device admin enabled successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Device admin permission denied", Toast.LENGTH_SHORT).show();
                }
                updateUI();
                break;
                
            case REQUEST_IGNORE_BATTERY_OPTIMIZATIONS:
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "Battery optimization disabled", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wsClient != null) {
            wsClient.disconnect();
        }
    }
}