# 📋 IdSiber-Eye Server - Dokumentasi Teknis

## 🎯 Overview
Server Node.js untuk sistem parental control IdSiber-Eye yang mengelola komunikasi antara portal web dan perangkat Android.

## 🚀 Fitur Utama
- **REST API** - Management devices dan commands
- **WebSocket (Socket.IO)** - Real-time communication  
- **Web Portal** - Interface untuk kontrol perangkat
- **SQLite Database** - Penyimpanan data persisten
- **Auto-reconnect** - Koneksi robust dengan fallback

## 🏗️ Architecture
```
[Web Portal] ↔ [HTTP/REST] ↔ Node.js Server ↔ [WebSocket] ↔ [Android Devices]
                    │               │
                    │               └── SQLite Database
                    └── Static File Serving
```

## 📦 Installation
```bash
# Install dependencies
npm install

# Jalankan server development
npm run dev

# Jalankan server production
npm start

# Inisialisasi database
npm run init-db
```

## ⚙️ Configuration
Copy file `.env.example` ke `.env` dan sesuaikan:
```env
PORT=3001
DB_PATH=./database.sqlite
JWT_SECRET=your-secret-key
LOG_LEVEL=info
```

## 🌐 API Endpoints

### Health Check
```http
GET /health
```
**Response:**
```json
{
  "status": "ok",
  "timestamp": "2024-01-15T10:30:00.000Z",
  "connectedDevices": 2
}
```

### Device Management
```http
GET    /api/devices                 # List semua devices
GET    /api/devices/:deviceId       # Detail device
GET    /api/devices/:deviceId/logs  # Activity logs
POST   /api/devices/:deviceId/command # Send command
```

### Quick Actions
```http
POST /api/devices/:deviceId/lock    # Lock device
POST /api/devices/:deviceId/unlock  # Unlock device  
POST /api/devices/:deviceId/reboot  # Reboot device
```

## 🔌 WebSocket Events (Socket.IO)

### Server → Client
- `real_time_update` - Command responses dan status updates
- `connect` / `disconnect` - Connection events

### Client → Server  
- `register_device` - Device registration
- `command_response` - Command execution results
- `status_update` - Device status updates
- `heartbeat` - Keep-alive

## 🗃️ Database Schema

### Tables:
- `devices` - Registered devices information
- `activity_logs` - Command execution history  
- `device_configs` - Device configurations

## 📁 Project Structure
```
├── src/
│   ├── models/
│   │   └── Database.js          # Database operations
│   ├── routes/
│   │   ├── devices.js           # Device management API
│   │   └── web.js               # Web portal serving
│   └── web/                     # Web portal files
│       └── portal.html          # Main portal interface
├── scripts/
│   └── init-db.js              # Database initialization
├── server.js                   # Main server file
└── database.sqlite             # SQLite database (auto-created)
```

## 🎮 Web Portal
Akses portal web di: `http://localhost:3001/`

### Fitur Portal:
- ✅ **Device Monitoring** - Real-time status devices
- ✅ **Remote Control** - 50+ commands executable
- ✅ **App Management** - Block/unblock applications  
- ✅ **Media Control** - Camera, audio, screenshot
- ✅ **Activity Logs** - History dan reporting
- ✅ **Responsive Design** - Mobile & desktop support

## 🔧 Development

### Menambah Route Baru:
1. Buat file di `src/routes/`
2. Export router function
3. Import dan register di `server.js`

### Menambah Database Table:
1. Edit `src/models/Database.js`
2. Tambah method untuk table baru
3. Update `initTables()` method

### Testing API:
Gunakan file Postman yang tersedia:
- `postman-collection.json` - API requests
- `postman-environment.json` - Environment variables

## 🚀 Deployment

### Docker Deployment:
```bash
docker-compose up -d
```

### Manual Deployment:
1. `npm install --production`
2. `npm start`
3. Setup reverse proxy (Nginx/Apache)
4. Configure SSL certificate

## 📊 Monitoring

### Log Types:
- **Connection logs** - Device connect/disconnect
- **Command logs** - API command executions  
- **Error logs** - System errors dan warnings
- **WebSocket logs** - Real-time communication

### Health Checks:
- Database connection status
- WebSocket server status  
- Memory usage monitoring
- Active connections count

## 🔐 Security Considerations

### Yang Sudah Diimplementasi:
- ✅ CORS configuration
- ✅ Rate limiting (100 requests/15 minutes)
- ✅ Input validation
- ✅ SQL injection prevention
- ✅ Secure headers (Helmet.js)

### Untuk Production:
- [ ] JWT authentication
- [ ] API key validation  
- [ ] HTTPS enforcement
- [ ] Request logging
- [ ] IP whitelisting

## 🐛 Troubleshooting

### Common Issues:
1. **Port already in use** - Change PORT in .env
2. **Database locked** - Delete database.sqlite and restart
3. **WebSocket errors** - Check firewall settings
4. **CORS errors** - Verify client URL configuration

### Logs Location:
- Console output untuk development
- File logging recommended untuk production

## 📞 Support

### Debug Mode:
Aktifkan debug mode dengan setting environment variable:
```bash
LOG_LEVEL=debug npm start
```

### Get Help:
1. Check console untuk error messages
2. Verify database connection
3. Test API endpoints dengan Postman
4. Check device WebSocket connection

## 🔄 Changelog

### v1.0.0 - Initial Release
- ✅ REST API untuk device management
- ✅ WebSocket real-time communication  
- ✅ Web portal interface
- ✅ SQLite database integration
- ✅ Auto-reconnect functionality
- ✅ Comprehensive logging

## 📄 License
Proprietary - IdSiber Eye Parental Control System

---
*Terakhir diupdate: ${new Date().toLocaleDateString('id-ID')}*
*Version: 1.0.0*