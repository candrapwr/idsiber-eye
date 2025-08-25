# 🚀 IdSiber-Eye Quick Start Guide

## Instalasi & Menjalankan Server

### 1. Install Dependencies
```bash
cd /Volumes/Workspace/nodeJs_app/idsiber-eye
npm install
```

### 2. Initialize Database
```bash
npm run init-db
```

### 3. Start Server
```bash
npm start
```

Server akan berjalan di: `http://localhost:3000`

### 4. Test Server (Optional)
```bash
# Di terminal baru
npm test
```

## 📱 Testing dengan Postman

### Import Collection
1. Buka Postman
2. Import file: `postman-collection.json`
3. Set variable `base_url` = `http://localhost:3000`
4. Set variable `device_id` = `test_device_123`

### Test Endpoints:

1. **Health Check**
   ```
   GET http://localhost:3000/health
   ```

2. **Lock HP Anak (30 menit)**
   ```
   POST http://localhost:3000/api/devices/android_device_123/lock
   Body: {"duration": 30}
   ```

3. **Unlock HP Anak**
   ```
   POST http://localhost:3000/api/devices/android_device_123/unlock
   ```

4. **Block Instagram**
   ```
   POST http://localhost:3000/api/devices/android_device_123/command
   Body: {
     "action": "block_app",
     "params": {
       "package_name": "com.instagram.android",
       "duration": 60
     }
   }
   ```

5. **Lihat Device yang Terdaftar**
   ```
   GET http://localhost:3000/api/devices
   ```

6. **Lihat Log Aktivitas**
   ```
   GET http://localhost:3000/api/devices/android_device_123/logs
   ```

## 📋 Command Actions yang Tersedia

| Action | Description | Parameters |
|--------|-------------|------------|
| `lock_screen` | Lock layar HP | `duration` (menit) |
| `unlock_screen` | Unlock layar | - |
| `reboot_device` | Restart HP | - |
| `set_volume` | Set volume | `volume` (0-100) |
| `block_app` | Block aplikasi | `package_name`, `duration` |
| `unblock_app` | Unblock aplikasi | `package_name` |
| `kill_app` | Force close app | `package_name` |
| `get_device_info` | Info device | - |
| `get_battery_status` | Status baterai | - |
| `get_installed_apps` | List aplikasi | - |
| `take_screenshot` | Screenshot | - |
| `enable_airplane_mode` | Aktifkan airplane mode | - |
| `disable_airplane_mode` | Matikan airplane mode | - |

## 🔧 Konfigurasi

Edit file `.env` (copy dari `.env.example`):
```bash
PORT=3000
DB_PATH=./database.sqlite
```

## 🐳 Docker (Optional)

```bash
# Build image
docker build -t idsiber-eye .

# Run container
docker run -p 3000:3000 -v $(pwd)/data:/app/data idsiber-eye

# Atau dengan docker-compose
docker-compose up -d
```

## 📱 Membuat Android Client

Untuk membuat aplikasi Android:

1. **Setup Project Android Studio**
2. **Add Dependencies** (build.gradle):
   ```gradle
   implementation 'io.socket:socket.io-client:2.1.0'
   implementation 'com.google.code.gson:gson:2.8.9'
   ```

3. **Copy Files Android Client**:
   - `WebSocketClient.java`
   - `CommandHandler.java`
   - `CommandResult.java`
   - `AndroidManifest.xml`

4. **Request Permissions** saat runtime
5. **Enable Device Admin** untuk lock screen
6. **Setup Accessibility Service** untuk kontrol app

## 🚨 Troubleshooting

### Server tidak bisa start
```bash
# Cek port 3000 tidak digunakan
lsof -i :3000

# Kill process jika ada
kill -9 <PID>
```

### Database error
```bash
# Hapus database dan buat ulang
rm database.sqlite
npm run init-db
```

### WebSocket connection failed
- Pastikan server berjalan
- Cek firewall/antivirus
- Gunakan IP address daripada localhost

## 📞 Support

Jika ada masalah:
1. Cek log server di console
2. Lihat log aktivitas di database
3. Test dengan Postman collection
4. Pastikan semua dependencies terinstall

## 🔒 Security Notes

- Aplikasi ini untuk pengawasan anak yang sah
- Pastikan izin orang tua sebelum install
- Gunakan HTTPS untuk production
- Implementasi authentication untuk keamanan

## 🎯 Next Steps

1. Install & test server ✅
2. Test dengan Postman ✅  
3. Develop Android client app
4. Deploy ke server production
5. Setup domain & SSL certificate
6. Implement web dashboard (optional)