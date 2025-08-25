package com.idsiber.eye;

import android.app.ActivityManager;
import android.content.Context;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Handler untuk menjalankan command yang diterima dari server
 */
public class CommandHandler {
    private static final String TAG = "CommandHandler";
    private Context context;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdminReceiver;
    
    public CommandHandler(Context context) {
        this.context = context;
        this.devicePolicyManager = (DevicePolicyManager) 
            context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.deviceAdminReceiver = new ComponentName(context, IdSiberDeviceAdminReceiver.class);
    }
    
    public CommandResult executeCommand(String action, JSONObject params) {
        Log.d(TAG, "Executing command: " + action);
        
        try {
            switch (action) {
                case "lock_screen":
                    return lockScreen(params);
                case "unlock_screen":
                    return unlockScreen();
                case "reboot_device":
                    return rebootDevice();
                case "set_volume":
                    return setVolume(params);
                case "get_device_info":
                    return getDeviceInfo();
                case "get_battery_status":
                    return getBatteryStatus();
                case "block_app":
                    return blockApp(params);
                case "unblock_app":
                    return unblockApp(params);
                case "kill_app":
                    return killApp(params);
                case "get_installed_apps":
                    return getInstalledApps();
                case "take_screenshot":
                    return takeScreenshot();
                case "enable_airplane_mode":
                    return enableAirplaneMode();
                case "disable_airplane_mode":
                    return disableAirplaneMode();
                default:
                    return new CommandResult(false, "Unknown command: " + action, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Command execution error", e);
            return new CommandResult(false, "Command execution failed: " + e.getMessage(), null);
        }
    }
    
    private CommandResult lockScreen(JSONObject params) {
        try {
            if (devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
                devicePolicyManager.lockNow();
                
                int duration = 0;
                if (params != null && params.has("duration")) {
                    duration = params.getInt("duration");
                    Log.d(TAG, "Screen locked for " + duration + " minutes");
                }
                
                return new CommandResult(true, "Screen locked successfully" + 
                    (duration > 0 ? " for " + duration + " minutes" : ""), null);
            } else {
                return new CommandResult(false, "Device admin permission required", null);
            }
        } catch (Exception e) {
            return new CommandResult(false, "Failed to lock screen: " + e.getMessage(), null);
        }
    }
    
    private CommandResult unlockScreen() {
        return new CommandResult(false, "Unlock not supported by Android security policy", null);
    }
    
    private CommandResult rebootDevice() {
        try {
            try {
                Process process = Runtime.getRuntime().exec("su -c reboot");
                return new CommandResult(true, "Reboot command sent (requires root)", null);
            } catch (Exception rootError) {
                return new CommandResult(false, "Reboot requires root access or system permissions", null);
            }
        } catch (Exception e) {
            return new CommandResult(false, "Reboot failed: " + e.getMessage(), null);
        }
    }
    
    private CommandResult setVolume(JSONObject params) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                return new CommandResult(false, "AudioManager not available", null);
            }
            
            int volume = params.getInt("volume");
            if (volume < 0 || volume > 100) {
                return new CommandResult(false, "Volume must be between 0-100", null);
            }
            
            int streamType = AudioManager.STREAM_MUSIC;
            int maxVolume = audioManager.getStreamMaxVolume(streamType);
            int targetVolume = (volume * maxVolume) / 100;
            
            audioManager.setStreamVolume(streamType, targetVolume, 0);
            Log.d(TAG, "Volume set to " + volume + "%");
            
            return new CommandResult(true, "Volume set to " + volume + "%", null);
        } catch (Exception e) {
            return new CommandResult(false, "Failed to set volume: " + e.getMessage(), null);
        }
    }
    
    private CommandResult getDeviceInfo() {
        try {
            JSONObject deviceInfo = new JSONObject();
            deviceInfo.put("device_model", Build.MODEL);
            deviceInfo.put("device_brand", Build.BRAND);
            deviceInfo.put("device_manufacturer", Build.MANUFACTURER);
            deviceInfo.put("android_version", Build.VERSION.RELEASE);
            deviceInfo.put("api_level", Build.VERSION.SDK_INT);
            deviceInfo.put("device_id", Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID));
            deviceInfo.put("device_admin_active", devicePolicyManager.isAdminActive(deviceAdminReceiver));
            
            return new CommandResult(true, "Device info retrieved", deviceInfo.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get device info: " + e.getMessage(), null);
        }
    }
    
    private CommandResult getBatteryStatus() {
        try {
            Intent batteryIntent = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            
            if (batteryIntent == null) {
                return new CommandResult(false, "Cannot access battery information", null);
            }
            
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            
            float batteryPct = level * 100 / (float) scale;
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
            
            JSONObject batteryInfo = new JSONObject();
            batteryInfo.put("battery_level", (int) batteryPct);
            batteryInfo.put("is_charging", isCharging);
            batteryInfo.put("status", getStatusString(status));
            
            return new CommandResult(true, "Battery status retrieved", batteryInfo.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get battery status: " + e.getMessage(), null);
        }
    }
    
    private String getStatusString(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING: return "Charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "Discharging";
            case BatteryManager.BATTERY_STATUS_FULL: return "Full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "Not Charging";
            case BatteryManager.BATTERY_STATUS_UNKNOWN: return "Unknown";
            default: return "Unknown";
        }
    }
    
    private CommandResult blockApp(JSONObject params) {
        try {
            String packageName = params.getString("package_name");
            return new CommandResult(false, "App blocking requires accessibility service or device owner permissions", null);
        } catch (Exception e) {
            return new CommandResult(false, "Failed to block app: " + e.getMessage(), null);
        }
    }
    
    private CommandResult unblockApp(JSONObject params) {
        try {
            String packageName = params.getString("package_name");
            return new CommandResult(false, "App unblocking requires accessibility service", null);
        } catch (Exception e) {
            return new CommandResult(false, "Failed to unblock app: " + e.getMessage(), null);
        }
    }
    
    private CommandResult killApp(JSONObject params) {
        try {
            String packageName = params.getString("package_name");
            ActivityManager activityManager = (ActivityManager) 
                context.getSystemService(Context.ACTIVITY_SERVICE);
            
            if (activityManager == null) {
                return new CommandResult(false, "ActivityManager not available", null);
            }
            
            activityManager.killBackgroundProcesses(packageName);
            Log.d(TAG, "Killed app: " + packageName);
            return new CommandResult(true, "App terminated: " + packageName, null);
            
        } catch (Exception e) {
            return new CommandResult(false, "Failed to kill app: " + e.getMessage(), null);
        }
    }
    
    private CommandResult getInstalledApps() {
        try {
            PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            
            JSONArray appList = new JSONArray();
            int userApps = 0;
            
            for (ApplicationInfo app : apps) {
                try {
                    JSONObject appInfo = new JSONObject();
                    appInfo.put("package_name", app.packageName);
                    appInfo.put("app_name", pm.getApplicationLabel(app).toString());
                    appInfo.put("is_system", (app.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
                    appInfo.put("enabled", app.enabled);
                    
                    appList.put(appInfo);
                    
                    if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        userApps++;
                    }
                } catch (Exception appError) {
                    Log.w(TAG, "Error processing app: " + app.packageName);
                }
            }
            
            JSONObject result = new JSONObject();
            result.put("apps", appList);
            result.put("total_apps", appList.length());
            result.put("user_apps", userApps);
            result.put("system_apps", appList.length() - userApps);
            
            return new CommandResult(true, "Found " + appList.length() + " installed apps", result.toString());
            
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get installed apps: " + e.getMessage(), null);
        }
    }
    
    private CommandResult takeScreenshot() {
        return new CommandResult(false, "Screenshot requires MediaProjection permission or root access", null);
    }
    
    private CommandResult enableAirplaneMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.Global.putInt(context.getContentResolver(), 
                    Settings.Global.AIRPLANE_MODE_ON, 1);
            } else {
                Settings.System.putInt(context.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 1);
            }
            
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", true);
            context.sendBroadcast(intent);
            
            return new CommandResult(true, "Airplane mode enabled", null);
            
        } catch (SecurityException e) {
            return new CommandResult(false, "Airplane mode control requires WRITE_SECURE_SETTINGS permission", null);
        } catch (Exception e) {
            return new CommandResult(false, "Failed to enable airplane mode: " + e.getMessage(), null);
        }
    }
    
    private CommandResult disableAirplaneMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.Global.putInt(context.getContentResolver(), 
                    Settings.Global.AIRPLANE_MODE_ON, 0);
            } else {
                Settings.System.putInt(context.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0);
            }
            
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", false);
            context.sendBroadcast(intent);
            
            return new CommandResult(true, "Airplane mode disabled", null);
            
        } catch (SecurityException e) {
            return new CommandResult(false, "Airplane mode control requires WRITE_SECURE_SETTINGS permission", null);
        } catch (Exception e) {
            return new CommandResult(false, "Failed to disable airplane mode: " + e.getMessage(), null);
        }
    }
}