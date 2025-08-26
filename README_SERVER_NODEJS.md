# 🚀 IdSiber Eye Server - Technical Documentation

## 🎯 Overview
Node.js server for the IdSiber Eye parental control system, providing real-time communication, device management, and web portal interface for monitoring and controlling Android devices remotely.

## 🏗️ Architecture
```
┌─────────────────────┐    HTTP/WebSocket    ┌─────────────────────┐
│ Parent (Web Portal) │ ◄──────────────────► │ Express.js Server   │
└─────────────────────┘                      │ Socket.IO WebSocket │
                                             │ SQLite Database     │
                                             └─────────────────────┘
                                                        │
┌─────────────────────┐   Real-time Commands   ┌─────────────────────┐
│ Android Devices     │ ◄───────────────────►  │ Device Management   │
│ (Child Devices)     │                        │ Activity Logging    │
└─────────────────────┘                        └─────────────────────┘
```

## 📦 Core Components

### 1. IdSiberEyeServer Class (server.js)
**Main server orchestrator with initialization and lifecycle management**
- **Express.js Integration**: REST API endpoints with middleware stack
- **Socket.IO Server**: Real-time WebSocket communication
- **Database Integration**: SQLite with automatic table initialization
- **Connection Management**: Track and manage connected Android devices
- **Health Monitoring**: Server status and performance metrics

### 2. Database Model (src/models/Database.js)
**SQLite database operations with comprehensive device and activity management**
```javascript
// Core database operations
- registerDevice(deviceInfo)          // Device registration
- updateDeviceStatus(deviceId, online) // Online/offline status
- logActivity(deviceId, action, status, message) // Command logging
- saveNotification(deviceId, notificationData)   // Notification storage
- getActivityLogs(deviceId, limit)    // Activity history
- getAllDevices()                     // Device inventory
```

### 3. Device Routes (src/routes/devices.js)
**RESTful API endpoints for device management and command execution**
- `GET /api/devices` - List all registered devices with online status
- `GET /api/devices/:deviceId` - Get specific device details  
- `GET /api/devices/:deviceId/logs` - Retrieve device activity logs
- `POST /api/devices/:deviceId/command` - Send custom commands
- `POST /api/devices/:deviceId/lock` - Quick lock device action
- `POST /api/devices/:deviceId/unlock` - Quick unlock device action
- `POST /api/devices/:deviceId/reboot` - Quick reboot device action

### 4. Web Portal Routes (src/routes/web.js)
**Static file serving and web portal interface**
- Portal HTML interface serving
- Static asset management (CSS, JS, images)
- Real-time dashboard for device monitoring

## 🔌 WebSocket Communication (Socket.IO)

### Server Events (Incoming from Devices)
```javascript
// Device registration
socket.on('register_device', async (deviceInfo) => {
    // Register device in database
    // Update online status
    // Store socket connection
    // Log connection activity
});

// Command response from device
socket.on('command_response', async (data) => {
    // Log command execution result
    // Broadcast real-time updates to web clients
    // Update device status
});

// Real-time notifications from device
socket.on('notification_event', async (notificationData) => {
    // Save notification to database
    // Broadcast to all connected web clients
    // Log notification activity
});

// Heartbeat for connection maintenance
socket.on('heartbeat', () => {
    socket.emit('heartbeat_response', { timestamp: Date.now() });
});
```

### Client Events (Outgoing to Devices)
```javascript
// Send command to device
socket.emit('command', {
    commandId: 'cmd_timestamp_random',
    action: 'lock_screen',
    params: { duration: 30 },
    timestamp: new Date().toISOString()
});

// Real-time updates to web clients
io.emit('real_time_update', {
    type: 'command_response',
    deviceId: 'device_123',
    action: 'lock_screen',
    success: true,
    result: {...},
    timestamp: new Date().toISOString()
});
```

## 🗃️ Database Schema

### Tables Structure
```sql
-- Registered devices
CREATE TABLE devices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT UNIQUE NOT NULL,
    device_name TEXT NOT NULL,
    device_model TEXT,
    android_version TEXT,
    app_version TEXT,
    is_online BOOLEAN DEFAULT 0,
    last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Activity logging
CREATE TABLE activity_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT NOT NULL,
    action TEXT NOT NULL,
    status TEXT NOT NULL,
    message TEXT,
    result TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices (device_id)
);

-- Notification storage
CREATE TABLE notifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT NOT NULL,
    package_name TEXT NOT NULL,
    title TEXT,
    text TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices (device_id)
);
```

## 🌐 REST API Endpoints

### Health & Status
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
GET /api/devices
```
**Response:**
```json
{
    "success": true,
    "devices": [
        {
            "device_id": "abc123",
            "device_name": "Child Phone",
            "device_model": "Samsung Galaxy S21",
            "android_version": "12",
            "is_connected": true,
            "connection_status": "online",
            "last_seen": "2024-01-15T10:30:00.000Z"
        }
    ],
    "total": 1,
    "online": 1
}
```

### Command Execution
```http
POST /api/devices/:deviceId/command
Content-Type: application/json

{
    "action": "lock_screen",
    "params": {
        "duration": 30
    }
}
```

**Response:**
```json
{
    "success": true,
    "commandId": "cmd_1642248600_abc123",
    "action": "lock_screen",
    "message": "Command 'lock_screen' sent to device abc123",
    "timestamp": "2024-01-15T10:30:00.000Z"
}
```

### Notification Management
```http
GET /api/notifications?device_id=abc123&limit=50
```
**Response:**
```json
{
    "success": true,
    "count": 25,
    "notifications": [
        {
            "id": 1,
            "device_id": "abc123",
            "package_name": "com.whatsapp",
            "title": "New Message",
            "text": "Hello from friend",
            "timestamp": "2024-01-15T10:25:00.000Z"
        }
    ]
}
```

## 🎮 Web Portal Features

### Real-time Dashboard
- **Device Status Monitoring**: Live online/offline status
- **Command Execution Interface**: 50+ available commands
- **Activity Logs Viewer**: Real-time command execution results
- **Notification Monitor**: Live notification stream from devices
- **Device Information Panel**: Hardware specs and system info

### Command Categories
```javascript
// Device Control Commands
{
    "lock_screen": {duration: 30},
    "set_volume": {volume: 50, stream: "music"},
    "set_brightness": {brightness: 75},
    "reboot_device": {},
    "get_battery_status": {}
}

// App Management Commands  
{
    "get_installed_apps": {max_apps: 100},
    "block_app": {package_name: "com.tiktok"},
    "kill_app": {package_name: "com.instagram.android"},
    "force_stop_app": {package_name: "com.snapchat.android"}
}

// Surveillance Commands
{
    "get_location": {},
    "start_audio_recording": {duration: 300},
    "take_screenshot": {},
    "get_contacts": {max_contacts: 500}
}

// System Monitoring Commands
{
    "get_usage_stats": {days: 7},
    "get_storage_info": {},
    "get_memory_info": {},
    "get_network_info": {}
}
```

## ⚙️ Configuration

### Environment Variables (.env)
```env
# Server Configuration
PORT=3001
HOST=0.0.0.0
SERVER_PUBLIC_IP=192.168.1.100

# Database
DB_PATH=./database.sqlite

# Security
JWT_SECRET=your-super-secret-key
RATE_LIMIT_WINDOW_MS=900000  # 15 minutes
RATE_LIMIT_MAX_REQUESTS=100

# Logging
LOG_LEVEL=info
```

### Server Initialization Options
```javascript
const server = new IdSiberEyeServer();
await server.start(3001, '0.0.0.0');  // Custom port and host
```

## 🔒 Security Features

### Implemented Security Measures
- ✅ **CORS Configuration**: Cross-origin resource sharing controls
- ✅ **Rate Limiting**: 100 requests per 15 minutes per IP
- ✅ **Input Validation**: JSON schema validation for all endpoints
- ✅ **SQL Injection Prevention**: Parameterized queries only
- ✅ **Secure Headers**: Helmet.js security headers (configurable)
- ✅ **Command Validation**: Server-side command parameter validation

### Recommended Production Security
```javascript
// Enable security middleware
app.use(helmet({
    contentSecurityPolicy: {
        directives: {
            defaultSrc: ["'self'"],
            scriptSrc: ["'self'", "'unsafe-inline'"],
            styleSrc: ["'self'", "'unsafe-inline'"]
        }
    },
    crossOriginEmbedderPolicy: false
}));

// JWT authentication middleware
app.use('/api', authenticateToken);

// API key validation
app.use('/api', validateApiKey);

// HTTPS enforcement
app.use((req, res, next) => {
    if (req.header('x-forwarded-proto') !== 'https') {
        res.redirect(`https://${req.header('host')}${req.url}`);
    } else {
        next();
    }
});
```

## 🚀 Deployment

### Development Mode
```bash
# Install dependencies
npm install

# Initialize database
npm run init-db

# Start development server (with nodemon)
npm run dev

# Test client connection
npm run test
```

### Production Deployment
```bash
# Install production dependencies only
npm install --production

# Initialize database
node scripts/init-db.js

# Start production server
npm start
```

### Docker Deployment
```dockerfile
# Dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install --production
COPY . .
EXPOSE 3001
CMD ["npm", "start"]
```

```bash
# Build and run
docker build -t idsiber-eye-server .
docker run -d -p 3001:3001 -v ./data:/app/data idsiber-eye-server

# Docker Compose
docker-compose up -d
```

### PM2 Process Management
```bash
# Install PM2
npm install -g pm2

# Start with PM2
pm2 start server.js --name "idsiber-eye-server"

# Monitor
pm2 monit

# Restart
pm2 restart idsiber-eye-server

# View logs
pm2 logs idsiber-eye-server
```

## 📊 Monitoring & Logging

### Built-in Monitoring
```javascript
// Health check endpoint provides:
{
    "status": "ok",
    "timestamp": "2024-01-15T10:30:00.000Z",
    "connectedDevices": 5,
    "uptime": "2 days, 14 hours",
    "memory": {
        "used": "245MB",
        "total": "512MB"
    },
    "database": {
        "status": "connected",
        "devices": 12,
        "logs": 1547
    }
}
```

### Log Categories
- **Connection Logs**: Device connect/disconnect events
- **Command Logs**: API command executions with results
- **Error Logs**: System errors, WebSocket failures, database errors
- **WebSocket Logs**: Real-time communication events
- **Activity Logs**: User actions and system operations

### External Monitoring Integration
```javascript
// Example: Integrate with monitoring service
app.use('/metrics', (req, res) => {
    const metrics = {
        active_connections: connectedDevices.size,
        total_commands: commandCount,
        error_rate: errorCount / totalRequests,
        response_time_avg: averageResponseTime
    };
    res.json(metrics);
});
```

## 🔧 Development

### Adding New API Endpoints
```javascript
// 1. Create new route in src/routes/
const express = require('express');
module.exports = (db, io, connectedDevices) => {
    const router = express.Router();
    
    router.get('/custom-endpoint', async (req, res) => {
        // Implementation
    });
    
    return router;
};

// 2. Register in server.js
const customRoutes = require('./src/routes/custom');
app.use('/api/custom', customRoutes(db, io, connectedDevices));
```

### Database Operations
```javascript
// Adding new database methods in src/models/Database.js
class Database {
    async customQuery(params) {
        const sql = `SELECT * FROM table WHERE condition = ?`;
        return new Promise((resolve, reject) => {
            this.db.all(sql, [params.value], (err, rows) => {
                if (err) reject(err);
                else resolve(rows);
            });
        });
    }
}
```

### WebSocket Events
```javascript
// Adding new WebSocket events
io.on('connection', (socket) => {
    socket.on('custom_event', async (data) => {
        // Handle custom event
        // Log to database
        // Broadcast to other clients if needed
    });
});
```

## 🧪 Testing

### API Testing with cURL
```bash
# Test health endpoint
curl http://localhost:3001/health

# Test device listing
curl http://localhost:3001/api/devices

# Send command to device
curl -X POST http://localhost:3001/api/devices/abc123/command \
  -H "Content-Type: application/json" \
  -d '{"action":"get_battery_status","params":{}}'

# Quick lock device
curl -X POST http://localhost:3001/api/devices/abc123/lock \
  -H "Content-Type: application/json" \
  -d '{"duration":60}'
```

### Postman Collection
Use the provided Postman files for comprehensive API testing:
- `postman-collection.json` - Complete API request collection
- `postman-environment.json` - Environment variables

### WebSocket Testing
```javascript
// Test WebSocket connection
const io = require('socket.io-client');
const socket = io('http://localhost:3001');

socket.on('connect', () => {
    console.log('Connected to server');
    
    // Simulate device registration
    socket.emit('register_device', {
        device_id: 'test_device_123',
        device_name: 'Test Device',
        device_model: 'Test Model',
        android_version: '12',
        app_version: '1.0.1'
    });
});
```

## 🐛 Troubleshooting

### Common Issues & Solutions

**1. Port Already in Use**
```bash
# Find process using port
lsof -i :3001

# Kill process
kill -9 <PID>

# Or change port in .env
PORT=3002
```

**2. Database Locked Error**
```bash
# Stop server
npm run stop

# Remove database file
rm database.sqlite

# Reinitialize
npm run init-db
npm start
```

**3. WebSocket Connection Issues**
- Check firewall settings (port 3001)
- Verify CORS configuration
- Ensure Socket.IO client version compatibility
- Check network connectivity between devices

**4. Memory Issues**
```bash
# Monitor memory usage
pm2 monit

# Restart if needed
pm2 restart idsiber-eye-server

# Check for memory leaks
node --inspect server.js
```

### Debug Mode
```bash
# Enable debug logging
LOG_LEVEL=debug npm start

# Socket.IO debug
DEBUG=socket.io* npm start

# All debug output
DEBUG=* npm start
```

### Database Maintenance
```bash
# Backup database
cp database.sqlite backups/database-$(date +%Y%m%d-%H%M).sqlite

# Check database integrity
sqlite3 database.sqlite "PRAGMA integrity_check;"

# Vacuum database (optimize)
sqlite3 database.sqlite "VACUUM;"

# View database stats
sqlite3 database.sqlite ".tables" ".schema" "SELECT COUNT(*) FROM devices;"
```

## 📈 Performance Optimization

### Connection Management
```javascript
// Optimize WebSocket connections
const io = socketIo(server, {
    pingTimeout: 60000,
    pingInterval: 25000,
    maxHttpBufferSize: 1e6,  // 1MB
    transports: ['websocket', 'polling']
});
```

### Database Optimization
```sql
-- Create indexes for better performance
CREATE INDEX idx_device_id ON activity_logs(device_id);
CREATE INDEX idx_timestamp ON activity_logs(timestamp);
CREATE INDEX idx_package_name ON notifications(package_name);
CREATE INDEX idx_notification_timestamp ON notifications(timestamp);
```

### Memory Management
```javascript
// Limit log retention
setInterval(async () => {
    await db.cleanOldLogs(7);  // Keep only 7 days
    await db.cleanOldNotifications(30);  // Keep only 30 days
}, 24 * 60 * 60 * 1000);  // Run daily
```

## 📦 Dependencies

### Core Dependencies
```json
{
    "express": "^4.18.2",          // Web server framework
    "socket.io": "^4.7.2",        // WebSocket server
    "sqlite3": "^5.1.6",          // Database
    "cors": "^2.8.5",             // Cross-origin requests
    "helmet": "^7.0.0",           // Security headers
    "express-rate-limit": "^6.9.0", // Rate limiting
    "uuid": "^9.0.0",             // Unique ID generation
    "dotenv": "^16.3.1"           // Environment variables
}
```

### Development Dependencies
```json
{
    "nodemon": "^3.0.1"           // Development auto-reload
}
```

## 📋 API Reference Summary

| Endpoint | Method | Description | Parameters |
|----------|--------|-------------|------------|
| `/health` | GET | Server health check | None |
| `/api/devices` | GET | List all devices | `limit`, `status` |
| `/api/devices/:id` | GET | Get device details | `deviceId` |
| `/api/devices/:id/logs` | GET | Get activity logs | `deviceId`, `limit` |
| `/api/devices/:id/command` | POST | Send custom command | `action`, `params` |
| `/api/devices/:id/lock` | POST | Quick lock device | `duration` |
| `/api/devices/:id/unlock` | POST | Quick unlock device | None |
| `/api/devices/:id/reboot` | POST | Quick reboot device | None |
| `/api/notifications` | GET | Get notifications | `device_id`, `limit`, `package_name` |
| `/api/notifications/:id` | DELETE | Delete notification | `notificationId` |
| `/api/notifications/device/:id` | DELETE | Clear device notifications | `deviceId` |

## 🚀 Production Checklist

### Pre-deployment
- [ ] Environment variables configured
- [ ] Database initialized and backed up
- [ ] SSL certificate installed
- [ ] Firewall rules configured
- [ ] Process manager configured (PM2)
- [ ] Monitoring and logging set up
- [ ] Rate limiting configured
- [ ] Security headers enabled

### Post-deployment
- [ ] Health check endpoint responding
- [ ] WebSocket connections working
- [ ] Database operations functional
- [ ] Log files rotating properly
- [ ] Performance monitoring active
- [ ] Backup schedule configured
- [ ] Documentation updated

## 📞 Support & Maintenance

### Log File Locations
```bash
# PM2 logs
~/.pm2/logs/idsiber-eye-server-out.log
~/.pm2/logs/idsiber-eye-server-error.log

# Application logs (if file logging enabled)
./logs/app.log
./logs/error.log
```

### Regular Maintenance Tasks
1. **Daily**: Monitor server health and active connections
2. **Weekly**: Review error logs and performance metrics
3. **Monthly**: Database cleanup and optimization
4. **Quarterly**: Security updates and dependency updates

### Emergency Procedures
```bash
# Quick restart
pm2 restart idsiber-eye-server

# Check server status
curl http://localhost:3001/health

# View recent logs
pm2 logs idsiber-eye-server --lines 50

# Database recovery
cp backups/database-latest.sqlite database.sqlite
pm2 restart idsiber-eye-server
```

## 🔗 Related Documentation
- [Android Client README](./README_CLIENT_ANDROID.md)
- [Main Project README](./README.md)
- [Postman Collection](./postman-collection.json)
- [Environment Setup](./scripts/)

## 📄 License
Proprietary - IdSiber Eye Parental Control System

---
*Last Updated: Aug 2025*  
*Version: 1.0.0*
*Server Framework: Node.js + Express.js + Socket.IO*
