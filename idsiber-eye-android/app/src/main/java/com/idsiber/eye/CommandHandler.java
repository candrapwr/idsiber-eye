package com.idsiber.eye;

import android.content.Context;
import android.util.Log;

import com.idsiber.eye.handlers.AppManagementHandler;
import com.idsiber.eye.handlers.DeviceControlHandler;
import com.idsiber.eye.handlers.FileManagementHandler;
import com.idsiber.eye.handlers.LocationHandler;
import com.idsiber.eye.handlers.MediaHandler;
import com.idsiber.eye.handlers.NetworkHandler;
import com.idsiber.eye.handlers.NotificationHandler;
import com.idsiber.eye.handlers.PersonalDataHandler;
import com.idsiber.eye.handlers.SystemInfoHandler;

import org.json.JSONObject;

/**
 * Enhanced CommandHandler untuk menjalankan command yang diterima dari server
 * Aplikasi Kontrol HP Android Anak - IdSiber Eye
 * 
 * Refactored dengan modular handlers untuk kemudahan maintenance
 */
public class CommandHandler {
    private static final String TAG = "CommandHandler";
    
    // Handler instances
    private DeviceControlHandler deviceControlHandler;
    private NetworkHandler networkHandler;
    private LocationHandler locationHandler;
    private MediaHandler mediaHandler;
    private SystemInfoHandler systemInfoHandler;
    private AppManagementHandler appManagementHandler;
    private PersonalDataHandler personalDataHandler;
    private NotificationHandler notificationHandler;
    private FileManagementHandler fileManagementHandler;
    
    public CommandHandler(Context context) {
        // Initialize all handlers
        deviceControlHandler = new DeviceControlHandler(context);
        networkHandler = new NetworkHandler(context);
        locationHandler = new LocationHandler(context);
        mediaHandler = new MediaHandler(context);
        systemInfoHandler = new SystemInfoHandler(context);
        appManagementHandler = new AppManagementHandler(context);
        personalDataHandler = new PersonalDataHandler(context);
        notificationHandler = new NotificationHandler(context);
        fileManagementHandler = new FileManagementHandler(context);
    }
    
    public CommandResult executeCommand(String action, JSONObject params) {
        Log.d(TAG, "Executing command: " + action);
        
        try {
            switch (action) {
                // ============= BASIC DEVICE CONTROL =============
                case "lock_screen":
                    return deviceControlHandler.lockScreen(params);
                case "unlock_screen":
                    return deviceControlHandler.unlockScreen();
                case "reboot_device":
                    return deviceControlHandler.rebootDevice();
                case "set_volume":
                    return deviceControlHandler.setVolume(params);
                case "mute_device":
                    return deviceControlHandler.muteDevice();
                case "unmute_device":
                    return deviceControlHandler.unmuteDevice();
                case "set_brightness":
                    return deviceControlHandler.setBrightness(params);
                case "set_screen_timeout":
                    return deviceControlHandler.setScreenTimeout(params);
                case "get_device_info":
                    return deviceControlHandler.getDeviceInfo();
                case "get_battery_status":
                    return deviceControlHandler.getBatteryStatus();
                
                // ============= NETWORK CONTROL =============
                case "enable_wifi":
                    return networkHandler.enableWifi();
                case "disable_wifi":
                    return networkHandler.disableWifi();
                case "enable_airplane_mode":
                    return networkHandler.enableAirplaneMode();
                case "disable_airplane_mode":
                    return networkHandler.disableAirplaneMode();
                case "get_network_info":
                    return networkHandler.getNetworkInfo();
                
                // ============= LOCATION SERVICES =============
                case "get_location":
                    return locationHandler.getLocation();
                case "enable_location":
                    return locationHandler.enableLocation();
                case "disable_location":
                    return locationHandler.disableLocation();
                
                // ============= MEDIA & RECORDING =============
                case "start_audio_recording":
                    return mediaHandler.startAudioRecording(params);
                case "stop_audio_recording":
                    return mediaHandler.stopAudioRecording();
                case "take_photo":
                    return mediaHandler.takePhoto(params);
                case "take_screenshot":
                    return mediaHandler.takeScreenshot();
                case "get_recording_status":
                    return mediaHandler.getRecordingStatus();
                case "list_recordings":
                    return mediaHandler.listRecordings();
                
                // ============= SYSTEM INFO =============
                case "get_storage_info":
                    return systemInfoHandler.getStorageInfo();
                case "get_memory_info":
                    return systemInfoHandler.getMemoryInfo();
                case "get_usage_stats":
                    return systemInfoHandler.getUsageStats(params);
                case "get_running_processes":
                    return systemInfoHandler.getRunningProcesses();
                
                // ============= APP MANAGEMENT =============
                case "get_installed_apps":
                    return appManagementHandler.getInstalledApps();
                case "block_app":
                    return appManagementHandler.blockApp(params);
                case "unblock_app":
                    return appManagementHandler.unblockApp(params);
                case "kill_app":
                    return appManagementHandler.killApp(params);
                case "force_stop_app":
                    return appManagementHandler.forceStopApp(params);
                case "disable_app":
                    return appManagementHandler.disableApp(params);
                case "enable_app":
                    return appManagementHandler.enableApp(params);
                case "clear_app_data":
                    return appManagementHandler.clearAppData(params);
                case "wipe_device":
                    return appManagementHandler.wipeDevice(params);
                case "get_app_info":
                    return appManagementHandler.getAppInfo(params);
                
                // ============= PERSONAL DATA =============
                case "get_contacts":
                    return personalDataHandler.getContacts();
                case "get_call_logs":
                    return personalDataHandler.getCallLogs();
                case "get_sms_messages":
                    return personalDataHandler.getSmsMessages();
                
                // ============= NOTIFICATION MANAGEMENT =============
                case "get_notifications":
                    return notificationHandler.getNotifications();
                case "clear_notifications":
                    return notificationHandler.clearNotifications();
                case "open_notification_settings":
                    return notificationHandler.openNotificationSettings();
                
                // ============= FILE MANAGEMENT =============
                case "list_files":
                    return fileManagementHandler.listFiles(params);
                case "delete_file":
                    return fileManagementHandler.deleteFile(params);
                case "get_file_info":
                    return fileManagementHandler.getFileInfo(params);
                
                // ============= COMMAND INFO =============
                case "get_available_commands":
                    return getAvailableCommands();
                case "get_command_help":
                    return getCommandHelp(params);
                
                default:
                    return new CommandResult(false, "Unknown command: " + action, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Command execution error", e);
            return new CommandResult(false, "Command execution failed: " + e.getMessage(), null);
        }
    }
    
    private CommandResult getAvailableCommands() {
        try {
            JSONObject commands = new JSONObject();
            
            // Device Control Commands
            JSONObject deviceCommands = new JSONObject();
            deviceCommands.put("lock_screen", "Lock device screen with optional duration");
            deviceCommands.put("unlock_screen", "Unlock device screen (not supported)");
            deviceCommands.put("reboot_device", "Reboot device (requires root)");
            deviceCommands.put("set_volume", "Set device volume (0-100) with optional stream type");
            deviceCommands.put("mute_device", "Mute all device audio streams");
            deviceCommands.put("unmute_device", "Unmute device and restore normal volumes");
            deviceCommands.put("set_brightness", "Set screen brightness (0-100)");
            deviceCommands.put("set_screen_timeout", "Set screen timeout in minutes");
            deviceCommands.put("get_device_info", "Get comprehensive device information");
            deviceCommands.put("get_battery_status", "Get detailed battery status and health");
            commands.put("device_control", deviceCommands);
            
            // Network Commands
            JSONObject networkCommands = new JSONObject();
            networkCommands.put("enable_wifi", "Enable WiFi (limited on Android 10+)");
            networkCommands.put("disable_wifi", "Disable WiFi (limited on Android 10+)");
            networkCommands.put("enable_airplane_mode", "Enable airplane mode");
            networkCommands.put("disable_airplane_mode", "Disable airplane mode");
            networkCommands.put("get_network_info", "Get comprehensive network information");
            commands.put("network_control", networkCommands);
            
            // Location Commands
            JSONObject locationCommands = new JSONObject();
            locationCommands.put("get_location", "Get current GPS/Network location");
            locationCommands.put("enable_location", "Enable location services (limited on Android 9+)");
            locationCommands.put("disable_location", "Disable location services (limited on Android 9+)");
            commands.put("location_services", locationCommands);
            
            // Media Commands
            JSONObject mediaCommands = new JSONObject();
            mediaCommands.put("start_audio_recording", "Start audio recording with optional duration");
            mediaCommands.put("stop_audio_recording", "Stop current audio recording");
            mediaCommands.put("take_photo", "Take photo with front/back camera");
            mediaCommands.put("take_screenshot", "Take device screenshot (requires special permissions)");
            mediaCommands.put("get_recording_status", "Get current recording status");
            mediaCommands.put("list_recordings", "List all recorded audio files");
            commands.put("media_recording", mediaCommands);
            
            // System Info Commands
            JSONObject systemCommands = new JSONObject();
            systemCommands.put("get_storage_info", "Get internal and external storage information");
            systemCommands.put("get_memory_info", "Get system and app memory usage");
            systemCommands.put("get_usage_stats", "Get app usage statistics (requires permission)");
            systemCommands.put("get_running_processes", "Get list of running processes");
            commands.put("system_info", systemCommands);
            
            // App Management Commands
            JSONObject appCommands = new JSONObject();
            appCommands.put("get_installed_apps", "Get list of all installed applications");
            appCommands.put("block_app", "Block/hide application (requires device admin)");
            appCommands.put("unblock_app", "Unblock/show application (requires device admin)");
            appCommands.put("kill_app", "Kill background processes of an app");
            appCommands.put("force_stop_app", "Force stop application (requires system permissions)");
            appCommands.put("disable_app", "Disable application (requires system permissions)");
            appCommands.put("enable_app", "Enable application (requires system permissions)");
            appCommands.put("clear_app_data", "Clear application data (requires system permissions)");
            appCommands.put("wipe_device", "Factory reset device (requires device admin)");
            appCommands.put("get_app_info", "Get detailed information about specific app");
            commands.put("app_management", appCommands);
            
            // Personal Data Commands
            JSONObject personalCommands = new JSONObject();
            personalCommands.put("get_contacts", "Get device contacts (requires permission)");
            personalCommands.put("get_call_logs", "Get call history (requires permission)");
            personalCommands.put("get_sms_messages", "Get SMS messages (requires permission)");
            commands.put("personal_data", personalCommands);
            
            // Notification Commands
            JSONObject notificationCommands = new JSONObject();
            notificationCommands.put("get_notifications", "Get current notifications (requires notification listener service)");
            notificationCommands.put("clear_notifications", "Clear app notifications");
            notificationCommands.put("open_notification_settings", "Open system notification listener settings");
            commands.put("notifications", notificationCommands);
            
            // File Management Commands
            JSONObject fileCommands = new JSONObject();
            fileCommands.put("list_files", "List files in specified directory");
            fileCommands.put("delete_file", "Delete file or directory");
            fileCommands.put("get_file_info", "Get detailed file information");
            commands.put("file_management", fileCommands);
            
            // Meta Commands
            JSONObject metaCommands = new JSONObject();
            metaCommands.put("get_available_commands", "Get list of all available commands");
            metaCommands.put("get_command_help", "Get help for specific command");
            commands.put("meta", metaCommands);
            
            return new CommandResult(true, "Available commands retrieved", commands.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get available commands: " + e.getMessage(), null);
        }
    }
    
    private CommandResult getCommandHelp(JSONObject params) {
        try {
            String command = params.getString("command");
            JSONObject help = new JSONObject();
            
            switch (command) {
                case "lock_screen":
                    help.put("description", "Lock device screen with optional duration");
                    help.put("parameters", "duration (optional): Duration in minutes to keep screen locked");
                    help.put("requires", "Device Admin permission");
                    help.put("example", "{\"duration\": 30}");
                    break;
                    
                case "set_volume":
                    help.put("description", "Set device volume for specified audio stream");
                    help.put("parameters", "volume (required): Volume level 0-100, stream (optional): music/ring/notification/alarm/call/system");
                    help.put("requires", "No special permissions");
                    help.put("example", "{\"volume\": 50, \"stream\": \"music\"}");
                    break;
                    
                case "get_location":
                    help.put("description", "Get current GPS or network-based location");
                    help.put("parameters", "None");
                    help.put("requires", "Location permission and location services enabled");
                    help.put("example", "{}");
                    break;
                    
                case "start_audio_recording":
                    help.put("description", "Start audio recording with optional duration limit");
                    help.put("parameters", "duration (optional): Maximum recording duration in seconds");
                    help.put("requires", "Audio recording and storage permissions");
                    help.put("example", "{\"duration\": 300}");
                    break;
                    
                case "get_usage_stats":
                    help.put("description", "Get app usage statistics for specified period");
                    help.put("parameters", "days (optional): Number of days to look back (default 1), max_apps (optional): Maximum apps to return (default 20)");
                    help.put("requires", "Usage stats permission");
                    help.put("example", "{\"days\": 7, \"max_apps\": 10}");
                    break;
                    
                case "block_app":
                    help.put("description", "Block/hide specified application");
                    help.put("parameters", "package_name (required): Package name of app to block");
                    help.put("requires", "Device Admin or Device Owner permissions");
                    help.put("example", "{\"package_name\": \"com.facebook.katana\"}");
                    break;
                    
                case "list_files":
                    help.put("description", "List files and directories in specified path");
                    help.put("parameters", "path (optional): Directory path (default external storage), max_files (optional): Maximum files to return (default 100), include_hidden (optional): Include hidden files (default false)");
                    help.put("requires", "Storage read permission");
                    help.put("example", "{\"path\": \"/sdcard/Download\", \"max_files\": 50}");
                    break;
                    
                default:
                    return new CommandResult(false, "No help available for command: " + command, null);
            }
            
            help.put("command", command);
            return new CommandResult(true, "Help retrieved for command: " + command, help.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get command help: " + e.getMessage(), null);
        }
    }
    
    /**
     * Cleanup method to be called when CommandHandler is destroyed
     */
    public void cleanup() {
        try {
            if (locationHandler != null) {
                locationHandler.cleanup();
            }
            if (mediaHandler != null) {
                mediaHandler.onDestroy();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
}
