# 🎯 IdSiberEye - Remote Android Parental Control

**IdSiberEye** is a robust parental control app for remotely managing your child's Android device. Monitor usage, control settings, and ensure a safe digital environment with real-time communication. ✅ **Core features fully functional** with an easy setup.

---

## 🚀 **What It Does**

IdSiberEye empowers parents to:
- 🔒 **Lock the screen** remotely with a timer.
- 🔊 **Adjust volume** (0-100%) from anywhere.
- ⚡ **Close apps** like games or social media.
- 📱 **View device details** (model, Android version, etc.).
- 🔋 **Monitor battery** (level, charging status).
- 📊 **Track activity** via logs stored in a database.

**Limitations**: Some features (e.g., reboot, airplane mode) require root access or system permissions. Others (e.g., app blocking, screenshots) are not yet supported.

**Planned Features** (not implemented):
- 📍 **Location Tracking**: Retrieve device location for emergency monitoring (requires location permissions, not yet implemented).
- 🔔 **Notification Monitoring**: Track notifications from specific apps (requires Notification Listener permission, planned for future release).

---

## 🏗️ **System Overview**

```
┌─────────────────────┐    HTTP/WebSocket    ┌─────────────────────┐
│ Parent (Web/Postman)│ ◄──────────────────► │ Node.js Server      │
└─────────────────────┘                      │ (192.168.8.179)     │
                                             └─────────────────────┘
                                                        │ SQLite
                                                        ▼
┌─────────────────────┐   Real-Time WebSocket  ┌─────────────────────┐
│ Android App         │ ◄───────────────────► │ Database            │
│ (Child's Device)    │                      │ - Devices, Logs     │
└─────────────────────┘                      └─────────────────────┘
```

- **Node.js Server**: Processes commands and stores logs.
- **Android App**: Runs on the child’s device with Device Admin privileges.
- **SQLite Database**: Stores device info and activity logs.
- **WebSocket**: Enables secure, real-time communication.

---

## 🚀 **Quick Setup**

### **1. Server Setup (~5 min)**
```bash
cd path..../idsiber-eye
npm install
npm run init-db
npm start
# Server runs at: http://192.168.8.179:3000
```

### **2. Android App (~10 min)**
1. Open Android Studio and import `idsiber-eye-android/`.
2. Build → Generate Signed APK.
3. Install APK on the child’s device.
4. Enable **Device Admin** and disable battery optimization.

### **3. Test It (~2 min)**
```bash
# Check server status
curl http://192.168.8.179:3000/health

# List devices
curl http://192.168.8.179:3000/api/devices

# Lock screen for 30 min
curl -X POST http://192.168.8.179:3000/api/devices/DEVICE_ID/lock \
  -H "Content-Type: application/json" \
  -d '{"duration": 30}'
```

---

## 📡 **API Overview**

**Base URL**: `http://192.168.8.179:3000`

### **Key Endpoints**
| Endpoint                     | Method | Description                     |
|------------------------------|--------|---------------------------------|
| `/health`                    | GET    | Check server status             |
| `/api/devices`               | GET    | List registered devices         |
| `/api/devices/:id`           | GET    | View device details             |
| `/api/devices/:id/logs`      | GET    | Get activity logs               |
| `/api/devices/:id/lock`      | POST   | Lock screen (duration in min)   |
| `/api/devices/:id/command`   | POST   | Send custom commands            |

### **Supported Commands**
```javascript
// Fully Functional
{"action": "set_volume", "params": {"volume": 50}} // Set volume (0-100%)
{"action": "kill_app", "params": {"package_name": "com.instagram.android"}} // Close app
{"action": "get_device_info", "params": {}} // Device details
{"action": "get_battery_status", "params": {}} // Battery info
{"action": "get_installed_apps", "params": {}} // List installed apps

// Limited Support (requires root or special permissions)
{"action": "reboot_device", "params": {}} // Requires root access
{"action": "enable_airplane_mode", "params": {}} // Requires WRITE_SECURE_SETTINGS
{"action": "disable_airplane_mode", "params": {}} // Requires WRITE_SECURE_SETTINGS

// Not Supported
{"action": "unlock_screen", "params": {}} // Blocked by Android security
{"action": "block_app", "params": {"package_name": "com.example"}} // Needs Accessibility Service
{"action": "unblock_app", "params": {"package_name": "com.example"}} // Needs Accessibility Service

// Planned (not yet implemented)
{"action": "get_location", "params": {}} // Requires location permissions
{"action": "monitor_notifications", "params": {}} // Requires Notification Listener
{"action": "take_screenshot", "params": {}} // Needs MediaProjection or root
```

---

## 🎯 **Example Usage**

### **Limit Gaming**
```bash
# Lock device for 2 hours
curl -X POST http://192.168.8.179:3000/api/devices/DEVICE_ID/lock \
  -H "Content-Type: application/json" \
  -d '{"duration": 120}'

# Close a game
curl -X POST http://192.168.8.179:3000/api/devices/DEVICE_ID/command \
  -H "Content-Type: application/json" \
  -d '{"action": "kill_app", "params": {"package_name": "com.pubg.krmobile"}}'
```

### **Bedtime Control**
```bash
# Set volume to silent
curl -X POST http://192.168.8.179:3000/api/devices/DEVICE_ID/command \
  -H "Content-Type: application/json" \
  -d '{"action": "set_volume", "params": {"volume": 0}}'

# Lock screen for 8 hours
curl -X POST http://192.168.8.179:3000/api/devices/DEVICE_ID/lock \
  -H "Content-Type: application/json" \
  -d '{"duration": 480}'
```

### **Emergency Monitoring (Planned)**
```bash
# Get device info
curl -X POST http://192.168.8.179:3000/api/devices/DEVICE_ID/command \
  -H "Content-Type: application/json" \
  -d '{"action": "get_device_info", "params": {}}'

# Get location (not yet implemented)
curl -X POST http://192.168.8.179:3000/api/devices/DEVICE_ID/command \
  -H "Content-Type: application/json" \
  -d '{"action": "get_location", "params": {}}' // Requires location permissions
```

---

## 🛠️ **Troubleshooting**

- **Connection Issues**:
  ```bash
  curl http://192.168.8.179:3000/health
  ping 192.168.8.179
  ```
  Ensure devices are on the same WiFi network.

- **Device Not Responding**:
  Verify the app is running (check notification), Device Admin is enabled, and battery optimization is disabled.

- **Debug Logs**:
  ```bash
  # Server logs
  DEBUG=* npm start
  # Android logs
  adb logcat -s "IdSiberEye" "WebSocketClient" "CommandHandler"
  ```

---

## 📱 **App Features**

- **User Interface**: Clean Material Design with real-time connection status.
- **Background Service**: Persistent with auto-reconnect and boot startup.
- **Security**: Device Admin rights, encrypted WebSocket, command validation.
- **Logging**: Tracks all actions in SQLite for transparency.

**Limitations**:
- Reboot and airplane mode require root or system permissions.
- App blocking, unblocking, and screenshots are not supported without advanced permissions.
- Location tracking and notification monitoring are planned but not yet implemented.

---

## 🚀 **Production Setup**

```bash
# Run with PM2
npm install -g pm2
pm2 start server.js --name idsiber-eye

# Docker
docker build -t idsiber-eye .
docker run -d -p 3000:3000 -v $(pwd)/data:/app/data idsiber-eye
```

**Backup Database**:
```bash
cp database.sqlite backups/database-$(date +%Y%m%d).sqlite
```

---

## ⚖️ **Legal & Ethical Use**

- ✅ **Purpose**: For parents to monitor and manage their children’s devices.
- ⚠️ **Disclaimer**: Use only on devices you own or have explicit consent for. Unauthorized use may violate privacy laws.

---

## 🏆 **Why IdSiberEye?**

- ✅ **Core Features Work**: Lock, volume, app termination, and monitoring are fully functional.
- ✅ **Real-Time**: Stable WebSocket for instant control.
- ✅ **Easy Setup**: ~5 min for server, ~10 min for app.
- ✅ **Future Features**: Location tracking and notification monitoring planned.

**IdSiberEye offers secure, reliable parental control for Android devices. Start managing today!** 👨‍👩‍👧‍👦✨