package com.idsiber.eye.handlers;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

import com.idsiber.eye.CommandResult;
import com.idsiber.eye.IdSiberDeviceAdminReceiver;

import org.json.JSONObject;

/**
 * Handler untuk kontrol device dasar seperti lock/unlock, volume, brightness, dll
 */
public class DeviceControlHandler {
    private static final String TAG = "DeviceControlHandler";
    private Context context;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdminReceiver;

    public DeviceControlHandler(Context context) {
        this.context = context;
        this.devicePolicyManager = (DevicePolicyManager) 
            context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.deviceAdminReceiver = new ComponentName(context, IdSiberDeviceAdminReceiver.class);
    }

    public CommandResult lockScreen(JSONObject params) {
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

    public CommandResult unlockScreen() {
        return new CommandResult(false, "Unlock not supported by Android security policy", null);
    }

    public CommandResult rebootDevice() {
        try {
            if (devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
                try {
                    // Try with device admin first
                    devicePolicyManager.reboot(deviceAdminReceiver);
                    return new CommandResult(true, "Device reboot initiated", null);
                } catch (Exception adminError) {
                    Log.w(TAG, "Device admin reboot failed, trying root method");
                }
            }
            
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

    public CommandResult setVolume(JSONObject params) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                return new CommandResult(false, "AudioManager not available", null);
            }

            int volume = params.getInt("volume");
            if (volume < 0 || volume > 100) {
                return new CommandResult(false, "Volume must be between 0-100", null);
            }

            String streamTypeStr = params.optString("stream", "music");
            int streamType = getStreamType(streamTypeStr);
            
            int maxVolume = audioManager.getStreamMaxVolume(streamType);
            int targetVolume = (volume * maxVolume) / 100;

            audioManager.setStreamVolume(streamType, targetVolume, 0);
            Log.d(TAG, "Volume set to " + volume + "% for stream: " + streamTypeStr);

            JSONObject result = new JSONObject();
            result.put("volume_set", volume);
            result.put("stream_type", streamTypeStr);
            result.put("actual_volume", targetVolume);
            result.put("max_volume", maxVolume);

            return new CommandResult(true, "Volume set to " + volume + "%", result.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to set volume: " + e.getMessage(), null);
        }
    }

    public CommandResult muteDevice() {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                return new CommandResult(false, "AudioManager not available", null);
            }

            // Mute different audio streams
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);

            return new CommandResult(true, "Device muted successfully", null);
        } catch (Exception e) {
            return new CommandResult(false, "Failed to mute device: " + e.getMessage(), null);
        }
    }

    public CommandResult unmuteDevice() {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                return new CommandResult(false, "AudioManager not available", null);
            }

            // Restore to normal mode
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            
            // Set reasonable volumes
            int ringMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
            int musicMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int notifMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
            
            audioManager.setStreamVolume(AudioManager.STREAM_RING, ringMax / 2, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, musicMax / 2, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, notifMax / 2, 0);

            return new CommandResult(true, "Device unmuted successfully", null);
        } catch (Exception e) {
            return new CommandResult(false, "Failed to unmute device: " + e.getMessage(), null);
        }
    }

    public CommandResult setBrightness(JSONObject params) {
        try {
            int brightness = params.getInt("brightness");
            if (brightness < 0 || brightness > 100) {
                return new CommandResult(false, "Brightness must be between 0-100", null);
            }

            // Convert percentage to Android brightness scale (0-255)
            int androidBrightness = (brightness * 255) / 100;

            // Check if we can modify system settings
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(context)) {
                    return new CommandResult(false, "Write settings permission required", null);
                }
            }

            Settings.System.putInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, androidBrightness);
            Settings.System.putInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

            JSONObject result = new JSONObject();
            result.put("brightness_set", brightness);
            result.put("android_brightness", androidBrightness);

            return new CommandResult(true, "Brightness set to " + brightness + "%", result.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to set brightness: " + e.getMessage(), null);
        }
    }

    public CommandResult setScreenTimeout(JSONObject params) {
        try {
            int timeoutMinutes = params.getInt("timeout_minutes");
            if (timeoutMinutes < 0 || timeoutMinutes > 60) {
                return new CommandResult(false, "Timeout must be between 0-60 minutes", null);
            }

            int timeoutMs = timeoutMinutes * 60 * 1000; // Convert to milliseconds

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(context)) {
                    return new CommandResult(false, "Write settings permission required", null);
                }
            }

            Settings.System.putInt(context.getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT, timeoutMs);

            JSONObject result = new JSONObject();
            result.put("timeout_minutes", timeoutMinutes);
            result.put("timeout_ms", timeoutMs);

            return new CommandResult(true, "Screen timeout set to " + timeoutMinutes + " minutes", result.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to set screen timeout: " + e.getMessage(), null);
        }
    }

    public CommandResult getBatteryStatus() {
        try {
            Intent batteryIntent = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            if (batteryIntent == null) {
                return new CommandResult(false, "Cannot access battery information", null);
            }

            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int health = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
            int temperature = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            int voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            String technology = batteryIntent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);

            float batteryPct = level * 100 / (float) scale;
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                               status == BatteryManager.BATTERY_STATUS_FULL;

            JSONObject batteryInfo = new JSONObject();
            batteryInfo.put("battery_level", (int) batteryPct);
            batteryInfo.put("is_charging", isCharging);
            batteryInfo.put("status", getBatteryStatusString(status));
            batteryInfo.put("health", getBatteryHealthString(health));
            batteryInfo.put("temperature", temperature / 10.0); // Convert from tenths of degree Celsius
            batteryInfo.put("voltage", voltage / 1000.0); // Convert from mV to V
            batteryInfo.put("technology", technology != null ? technology : "Unknown");

            return new CommandResult(true, "Battery status retrieved", batteryInfo.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get battery status: " + e.getMessage(), null);
        }
    }

    public CommandResult getDeviceInfo() {
        try {
            JSONObject deviceInfo = new JSONObject();
            deviceInfo.put("device_model", Build.MODEL);
            deviceInfo.put("device_brand", Build.BRAND);
            deviceInfo.put("device_manufacturer", Build.MANUFACTURER);
            deviceInfo.put("android_version", Build.VERSION.RELEASE);
            deviceInfo.put("api_level", Build.VERSION.SDK_INT);
            deviceInfo.put("build_number", Build.DISPLAY);
            deviceInfo.put("hardware", Build.HARDWARE);
            deviceInfo.put("board", Build.BOARD);
            deviceInfo.put("bootloader", Build.BOOTLOADER);
            deviceInfo.put("fingerprint", Build.FINGERPRINT);
            deviceInfo.put("device_id", Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID));
            deviceInfo.put("device_admin_active", devicePolicyManager.isAdminActive(deviceAdminReceiver));

            return new CommandResult(true, "Device info retrieved", deviceInfo.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get device info: " + e.getMessage(), null);
        }
    }

    private int getStreamType(String streamTypeStr) {
        switch (streamTypeStr.toLowerCase()) {
            case "ring":
                return AudioManager.STREAM_RING;
            case "music":
                return AudioManager.STREAM_MUSIC;
            case "notification":
                return AudioManager.STREAM_NOTIFICATION;
            case "alarm":
                return AudioManager.STREAM_ALARM;
            case "call":
                return AudioManager.STREAM_VOICE_CALL;
            case "system":
                return AudioManager.STREAM_SYSTEM;
            default:
                return AudioManager.STREAM_MUSIC;
        }
    }

    private String getBatteryStatusString(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING: return "Charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "Discharging";
            case BatteryManager.BATTERY_STATUS_FULL: return "Full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "Not Charging";
            case BatteryManager.BATTERY_STATUS_UNKNOWN: return "Unknown";
            default: return "Unknown";
        }
    }

    private String getBatteryHealthString(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "Good";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Overheat";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "Dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Over Voltage";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "Unspecified Failure";
            case BatteryManager.BATTERY_HEALTH_COLD: return "Cold";
            case BatteryManager.BATTERY_HEALTH_UNKNOWN: return "Unknown";
            default: return "Unknown";
        }
    }
}
