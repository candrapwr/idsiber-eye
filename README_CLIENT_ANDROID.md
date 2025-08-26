# 📱 IdSiber Eye Android Client - Technical Documentation

## 🎯 Overview
The IdSiber Eye Android client is a comprehensive parental control agent that runs on child devices, enabling remote monitoring and control via WebSocket communication with the Node.js server.

## 🏗️ Architecture
```
[MainActivity] → [WebSocketClient] → [WebSocketService] 
       ↓                ↓                    ↓
[ServerConfig]    [CommandHandler]    [NotificationService]
       ↓                ↓                    ↓
[Device Admin]   [Handler Classes]   [Background Service]
```

## ⚙️ Core Components

### 1. MainActivity.java
**Main application interface with setup management**
- **Device Admin Setup**: Requests device administrator privileges
- **Permission Management**: Handles all required permissions (camera, location, storage, etc.)
- **Notification Listener**: Configures notification access permissions  
- **Server Configuration**: Dynamic server IP and port configuration
- **Battery Optimization**: Disables battery optimization for background operation
- **Connection Management**: WebSocket connection controls

### 2. WebSocketClient.java & WebSocketService.java
**Real-time communication with server**
- **Auto-reconnect**: Intelligent reconnection with exponential backoff
- **Background Service**: Persistent foreground service for 24/7 operation
- **Heartbeat System**: Keep-alive mechanism (30-second intervals)
- **Command Processing**: Routes incoming commands to appropriate handlers
- **Status Reporting**: Real-time device status updates to server

### 3. CommandHandler.java
**Central command coordinator routing 50+ commands to specialized handlers**
```java
// Command routing system
public CommandResult handleCommand(String action, JSONObject params) {
    switch (action) {
        case "lock_screen": return deviceControlHandler.lockScreen(params);
        case "get_location": return locationHandler.getLocation();
        case "start_audio_recording": return mediaHandler.startAudioRecording(params);
        // ... 47 more commands
    }
}
```

## 🔧 Handler Classes (Modular Architecture)

### DeviceControlHandler.java
**Basic device control operations**
- ✅ `lock_screen` - Lock device with optional duration
- ✅ `reboot_device` - Device reboot (requires root/admin)
- ✅ `set_volume` - Volume control (0-100%) for different streams
- ✅ `mute_device` / `unmute_device` - Audio muting
- ✅ `set_brightness` - Screen brightness control (0-100%)
- ✅ `set_screen_timeout` - Screen timeout configuration
- ✅ `get_device_info` - Complete device information
- ✅ `get_battery_status` - Battery level, health, temperature, voltage

### AppManagementHandler.java
**Application lifecycle and control**
- ✅ `get_installed_apps` - List all installed applications with details
- ✅ `block_app` / `unblock_app` - App blocking (requires device admin)
- ✅ `kill_app` - Terminate background processes
- ✅ `force_stop_app` - Force application termination
- ✅ `disable_app` / `enable_app` - App state management
- ✅ `clear_app_data` - App data clearing (requires device owner)
- ✅ `wipe_device` - Factory reset (DANGEROUS - requires device admin)
- ✅ `get_app_info` - Detailed application information

### LocationHandler.java
**GPS and network-based location services**
- ✅ `get_location` - GPS/Network location with intelligent fallback
- ✅ `enable_location` / `disable_location` - Location services control
- **Features**: LocationListener implementation, best location algorithm
- **Providers**: GPS, Network, Passive location providers
- **Accuracy**: Intelligent provider selection based on accuracy and freshness

### MediaHandler.java
**Audio, photo, and video recording capabilities**
- ✅ `start_audio_recording` / `stop_audio_recording` - Background audio recording
- ✅ `take_photo` - Camera capture (front/back)  
- ✅ `take_screenshot` - Screen capture (requires special permissions)
- ✅ `get_recording_status` - Recording status monitoring
- ✅ `list_recordings` - File management for recorded media
- **File Organization**: `/sdcard/IdSiberEye/Recordings/`, `/sdcard/IdSiberEye/Photos/`

### NetworkHandler.java
**Network connectivity and airplane mode control**
- ✅ `get_network_info` - WiFi details, mobile operator, IP addresses
- ✅ `enable_wifi` / `disable_wifi` - WiFi control (limited on Android 10+)
- ✅ `enable_airplane_mode` / `disable_airplane_mode` - Flight mode control
- **Network Types**: WiFi, Mobile Data, Ethernet detection
- **Operator Info**: Mobile network operator details

### SystemInfoHandler.java
**System monitoring and performance metrics**
- ✅ `get_storage_info` - Internal/external storage with usage percentages
- ✅ `get_memory_info` - RAM usage, available memory, app-specific usage
- ✅ `get_usage_stats` - App usage statistics (requires usage access permission)
- ✅ `get_running_processes` - Active processes with importance levels
- **Performance Metrics**: CPU usage, uptime, detailed memory analysis

### PersonalDataHandler.java
**Contact, SMS, and call log access**
- ✅ `get_contacts` - Device contacts with phone number types
- ✅ `get_call_logs` - Call history with duration and location data
- ✅ `get_sms_messages` - SMS messages with thread information
- **Privacy Handling**: Secure content provider access

### FileManagementHandler.java
**File system operations and management**
- ✅ `list_files` - Directory listing with filtering and sorting
- ✅ `delete_file` - File/directory deletion (recursive)
- ✅ `get_file_info` - Detailed file information with MIME types
- **Features**: Size formatting, MIME type detection, permission validation

### NotificationHandler.java
**Notification monitoring and management**
- ✅ `get_notifications` - Notification access (requires NotificationListenerService)
- ✅ `clear_notifications` - Clear specific app notifications
- **Integration**: IdSiberNotificationListener service for real-time monitoring

## 🔐 Security & Permissions

### Required Permissions
```xml
<!-- Critical Permissions -->
<uses-permission android:name="android.permission.DEVICE_ADMIN" />
<uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" />

<!-- Media & Storage -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

<!-- Location -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Personal Data -->
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />

<!-- System -->
<uses-permission android:name="android.permission.USAGE_STATS" />
<uses-permission android:name="android.permission.WRITE_SETTINGS" />
```

### Device Admin Capabilities
```java
<device-admin>
    <uses-policies>
        <limit-password />
        <watch-login />
        <reset-password />
        <force-lock />
        <wipe-data />
        <expire-password />
        <encrypted-storage />
        <disable-camera />
    </uses-policies>
</device-admin>
```

## 🔄 WebSocket Communication Protocol

### Device Registration
```javascript
{
    "event": "register_device",
    "data": {
        "device_id": "unique_android_id",
        "device_name": "Child Phone",
        "device_model": "Samsung Galaxy S21",
        "android_version": "12",
        "app_version": "1.0.1"
    }
}
```

### Command Execution Flow
```javascript
// Server → Device
{
    "event": "command",
    "data": {
        "commandId": "cmd_123456789",
        "action": "lock_screen",
        "params": {"duration": 30},
        "timestamp": "2024-01-15T10:30:00.000Z"
    }
}

// Device → Server
{
    "event": "command_response", 
    "data": {
        "commandId": "cmd_123456789",
        "action": "lock_screen",
        "success": true,
        "message": "Screen locked for 30 minutes",
        "result": "{\"locked_until\": \"2024-01-15T11:00:00.000Z\"}"
    }
}
```

### Real-time Notifications
```javascript
{
    "event": "notification_event",
    "data": {
        "device_id": "unique_android_id",
        "notification_data": {
            "package_name": "com.whatsapp",
            "title": "New Message",
            "text": "Hello from friend",
            "timestamp": 1642248600000
        }
    }
}
```

## 📋 Command Examples

### Device Control
```javascript
// Lock device for 2 hours
{"action": "lock_screen", "params": {"duration": 120}}

// Set music volume to 50%
{"action": "set_volume", "params": {"volume": 50, "stream": "music"}}

// Set brightness to maximum
{"action": "set_brightness", "params": {"brightness": 100}}
```

### App Management
```javascript
// Block TikTok app
{"action": "block_app", "params": {"package_name": "com.zhiliaoapp.musically"}}

// Kill YouTube background process
{"action": "kill_app", "params": {"package_name": "com.google.android.youtube"}}

// Get installed games (system apps excluded)
{"action": "get_installed_apps", "params": {"include_system": false}}
```

### Surveillance & Monitoring
```javascript
// Record audio for 5 minutes
{"action": "start_audio_recording", "params": {"duration": 300}}

// Get current precise location
{"action": "get_location", "params": {}}

// Get app usage for last week
{"action": "get_usage_stats", "params": {"days": 7, "max_apps": 20}}

// Take front camera photo
{"action": "take_photo", "params": {"camera": "front"}}
```

### File & Data Access
```javascript
// List downloads folder
{"action": "list_files", "params": {"path": "/sdcard/Download"}}

// Get all contacts
{"action": "get_contacts", "params": {"max_contacts": 500}}

// Get last 50 SMS messages
{"action": "get_sms_messages", "params": {"limit": 50}}
```

## 🛠️ Configuration

### Server Configuration (ServerConfig.java)
- **Dynamic IP/Port**: Configurable server endpoint
- **SharedPreferences**: Persistent configuration storage
- **Validation**: Input validation for IP addresses and ports
- **Default Values**: Fallback to Constants.java values

### Constants.java
```java
public static final String SERVER_PUBLIC_IP = "10.88.66.40";
public static final int PORT = 3001;
public static final int SOCKET_TIMEOUT = 10000;  // 10 seconds
public static final int HEARTBEAT_INTERVAL = 30000;  // 30 seconds
public static final String APP_VERSION = "1.0.1";
```

## 🔧 Development & Building

### Android Studio Setup
1. **Import Project**: Open `idsiber-eye-android` in Android Studio
2. **SDK Requirements**: Android API 21+ (Android 5.0 Lollipop)
3. **Dependencies**: All dependencies defined in `build.gradle`
4. **Build Variants**: Debug and Release configurations

### APK Building
```bash
# Debug APK
./gradlew assembleDebug

# Release APK (signed)
./gradlew assembleRelease

# Install to connected device
./gradlew installDebug
```

### Testing
```bash
# Unit tests
./gradlew test

# Connected device tests  
./gradlew connectedAndroidTest

# Lint checking
./gradlew lint
```

## 🐛 Troubleshooting

### Common Issues
1. **Device Admin Not Working**: Check if device admin is properly enabled in Settings
2. **Notifications Not Captured**: Ensure NotificationListenerService is enabled  
3. **Location Not Available**: Verify location permissions and GPS is enabled
4. **Audio Recording Fails**: Check microphone permission and storage space
5. **WebSocket Disconnections**: Verify server address and network connectivity

### Debug Logging
```java
// Enable verbose logging
Log.d("IdSiberEye", "Debug message");
Log.i("WebSocketClient", "Info message");
Log.e("CommandHandler", "Error message", exception);
```

### ADB Debugging
```bash
# Filter IdSiberEye logs
adb logcat -s "IdSiberEye" "WebSocketClient" "CommandHandler"

# Clear logs and monitor
adb logcat -c && adb logcat
```

## 📱 Supported Android Versions
- **Minimum**: Android 5.0 (API 21)
- **Target**: Android 13 (API 33)
- **Tested**: Android 6.0 - 13.0
- **Compatibility**: Graceful degradation for older versions

## 🚀 Deployment

### Installation Steps
1. **Enable Unknown Sources**: Allow installation from unknown sources
2. **Install APK**: Install the generated APK file
3. **Grant Permissions**: Accept all permission requests
4. **Enable Device Admin**: Activate device administrator
5. **Configure Server**: Set correct server IP and port
6. **Test Connection**: Verify WebSocket connection

### Production Considerations
- **Obfuscation**: Enable ProGuard/R8 for release builds
- **Signing**: Use proper signing keys for release APKs
- **Updates**: Implement auto-update mechanism
- **Monitoring**: Add crash reporting (Firebase Crashlytics)

## 📄 License
Proprietary - IdSiber Eye Parental Control System

---
*Last Updated: Aug 2025*  
*Version: 1.0.1*
