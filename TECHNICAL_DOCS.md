# 📋 IdSiber-Eye - Technical Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Database Schema](#database-schema)
4. [API Documentation](#api-documentation)
5. [WebSocket Protocol](#websocket-protocol)
6. [Android Client Architecture](#android-client-architecture)
7. [Security Considerations](#security-considerations)
8. [Development Guide](#development-guide)
9. [Deployment](#deployment)
10. [Troubleshooting](#troubleshooting)

## Project Overview

### Purpose
IdSiber-Eye adalah sistem parental control untuk mengontrol dan memantau perangkat Android anak dari jarak jauh menggunakan teknologi WebSocket dan REST API.

### Core Features
- **Remote Device Control**: Lock/unlock, reboot, volume control
- **Application Management**: Block/unblock apps, kill processes
- **Real-time Monitoring**: Battery status, device info, location
- **Activity Logging**: Semua aktivitas dicatat dengan timestamp
- **Multi-device Support**: Dapat mengelola beberapa device sekaligus

### Technology Stack
- **Backend**: Node.js, Express.js, Socket.IO
- **Database**: SQLite3
- **Communication**: WebSocket (real-time), REST API
- **Security**: Helmet.js, Rate Limiting, CORS
- **Android**: Java/Kotlin dengan Socket.IO Client

## Architecture

### System Architecture Diagram
```
┌─────────────────┐    WebSocket/HTTP    ┌─────────────────┐
│   Parent App    │ ◄──────────────────► │   Node.js       │
│   (Web/Mobile)  │                      │   Server        │
└─────────────────┘                      └─────────────────┘
                                                   │
                                                   │ SQLite
                                                   ▼
                                         ┌─────────────────┐
        WebSocket Connection             │    Database     │
┌─────────────────┐ ◄──────────────────► │   - devices     │
│  Android Client │                      │   - logs        │
│  (Child Device) │                      │   - configs     │
└─────────────────┘                      └─────────────────┘
```

### Component Overview

#### 1. Node.js Server (`server.js`)
- **Express Server**: REST API endpoints
- **Socket.IO Server**: Real-time WebSocket communication
- **Middleware**: Security, CORS, rate limiting
- **Connection Management**: Track connected devices

#### 2. Database Layer (`src/models/Database.js`)
- **SQLite Operations**: CRUD operations with promises
- **Connection Management**: Pool management dan cleanup
- **Schema Management**: Table creation dan migration

#### 3. Route Handlers (`src/routes/devices.js`)
- **Device Management**: Registration, status, listing
- **Command Execution**: Send commands to devices
- **Log Retrieval**: Activity logs dan reporting

#### 4. Android Client
- **WebSocket Client**: Real-time connection ke server
- **Command Handler**: Execute received commands
- **Device Admin**: System-level permissions
- **Accessibility Service**: App control capabilities

## Database Schema

### ERD (Entity Relationship Diagram)
```
devices (1) ──┬─── (N) activity_logs
              └─── (N) device_configs

devices:
├── id (PK)
├── device_id (UNIQUE)
├── device_name
├── device_model
├── android_version
├── is_online
├── last_seen
└── created_at

activity_logs:
├── id (PK)
├── device_id (FK → devices.device_id)
├── action
├── status
├── message
└── timestamp

device_configs:
├── id (PK)
├── device_id (FK → devices.device_id)
├── config_key
├── config_value
└── updated_at
```

### Table Specifications

#### `devices` Table
```sql
CREATE TABLE devices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT UNIQUE NOT NULL,           -- Android device identifier
    device_name TEXT NOT NULL,                -- Display name
    device_model TEXT,                        -- Device model (e.g., "SM-A505F")
    android_version TEXT,                     -- Android version (e.g., "11")
    is_online BOOLEAN DEFAULT 0,              -- Current connection status
    last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

#### `activity_logs` Table
```sql
CREATE TABLE activity_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT NOT NULL,
    action TEXT NOT NULL,                     -- Command action (e.g., "lock_screen")
    status TEXT NOT NULL,                     -- sent/success/failed/info
    message TEXT,                             -- Detail message or error
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices (device_id)
);
```

#### `device_configs` Table
```sql
CREATE TABLE device_configs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT NOT NULL,
    config_key TEXT NOT NULL,                 -- Configuration key
    config_value TEXT,                        -- Configuration value (JSON)
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (device_id) REFERENCES devices (device_id),
    UNIQUE(device_id, config_key)
);
```

## API Documentation

### Base URL
```
http://localhost:3000
```

### Authentication
Currently no authentication implemented. For production, implement JWT or API key authentication.

### Endpoints

#### Health Check
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

#### Device Management

##### Get All Devices
```http
GET /api/devices
```
**Response:**
```json
{
  "success": true,
  "devices": [
    {
      "id": 1,
      "device_id": "android_device_123",
      "device_name": "Samsung A50",
      "device_model": "SM-A505F",
      "android_version": "11",
      "is_online": 1,
      "is_connected": true,
      "connection_status": "online",
      "last_seen": "2024-01-15T10:29:45.000Z",
      "created_at": "2024-01-15T08:00:00.000Z"
    }
  ],
  "total": 1,
  "online": 1
}
```

##### Get Device Details
```http
GET /api/devices/{deviceId}
```

##### Send Command
```http
POST /api/devices/{deviceId}/command
Content-Type: application/json

{
  "action": "lock_screen",
  "params": {
    "duration": 60
  }
}
```

##### Quick Actions
```http
POST /api/devices/{deviceId}/lock        # Lock device
POST /api/devices/{deviceId}/unlock      # Unlock device
POST /api/devices/{deviceId}/reboot      # Reboot device
```

#### Activity Logs

##### Get Device Logs
```http
GET /api/devices/{deviceId}/logs?limit=50
```

##### Get All Logs
```http
GET /api/devices/logs/all?limit=100
```

### Error Handling
All endpoints return consistent error format:
```json
{
  "success": false,
  "message": "Error description",
  "error_code": "DEVICE_NOT_FOUND"
}
```

## WebSocket Protocol

### Connection Flow
```
Client                          Server
  │                               │
  │──── connect ───────────────► │
  │                               │
  │──── register_device ────────► │
  │                               │
  │◄─── registration_success ──── │
  │                               │
  │◄─── command ────────────────── │
  │                               │
  │──── command_response ────────► │
  │                               │
  │◄──► heartbeat ◄──────────────► │
```

### Events Specification

#### Client → Server Events

##### `register_device`
```javascript
{
  device_id: "android_device_123",
  device_name: "Samsung A50",
  device_model: "SM-A505F",
  android_version: "11"
}
```

##### `command_response`
```javascript
{
  commandId: "cmd_1642234567890",
  action: "lock_screen",
  success: true,
  message: "Screen locked successfully",
  result: null
}
```

##### `status_update`
```javascript
{
  battery_level: 85,
  is_charging: false,
  screen_on: true,
  current_app: "com.android.launcher"
}
```

##### `heartbeat`
```javascript
{
  timestamp: 1642234567890
}
```

#### Server → Client Events

##### `registration_success`
```javascript
{
  message: "Device registered successfully",
  deviceId: "android_device_123"
}
```

##### `registration_error`
```javascript
{
  message: "Registration failed",
  error: "Device ID already exists"
}
```

##### `command`
```javascript
{
  commandId: "cmd_1642234567890",
  action: "lock_screen",
  params: {
    duration: 60
  },
  timestamp: "2024-01-15T10:30:00.000Z"
}
```

##### `heartbeat_response`
```javascript
{
  timestamp: 1642234567890
}
```

## Android Client Architecture

### Core Components

#### 1. WebSocketClient.java
```java
public class WebSocketClient {
    private Socket socket;
    private Context context;
    private CommandHandler commandHandler;
    
    // Connection management
    public void connect();
    public void disconnect();
    public void registerDevice();
    
    // Event handlers
    private void setupSocketListeners();
    private void handleCommand(JSONObject command);
    private void sendCommandResponse(String commandId, String action, CommandResult result);
}
```

#### 2. CommandHandler.java
```java
public class CommandHandler {
    // System controls
    private CommandResult lockScreen(JSONObject params);
    private CommandResult rebootDevice();
    private CommandResult setVolume(JSONObject params);
    
    // App controls
    private CommandResult blockApp(JSONObject params);
    private CommandResult killApp(JSONObject params);
    
    // Information gathering
    private CommandResult getDeviceInfo();
    private CommandResult getBatteryStatus();
}
```

#### 3. CommandResult.java
```java
public class CommandResult {
    private boolean success;
    private String message;
    private String data;
    
    // Getters and constructors
}
```

### Required Permissions
```xml
<!-- Network -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- System -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- Device Admin -->
<uses-permission android:name="android.permission.BIND_DEVICE_ADMIN" />

<!-- Audio Control -->
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />

<!-- Process Management -->
<uses-permission android:name="android.permission.KILL_BACKGROUND_PROCESSES" />
```

### Services Required

#### 1. WebSocketService
- Maintains persistent connection to server
- Handles reconnection logic
- Runs in foreground with notification

#### 2. DeviceAdminReceiver
- Enables lock screen functionality
- Handles device admin events
- Required for system-level controls

#### 3. AccessibilityService
- App blocking capabilities
- UI interaction automation
- Process monitoring

## Security Considerations

### Current Implementation
- **Rate Limiting**: 100 requests per 15 minutes per IP
- **CORS Protection**: Configurable origin restrictions
- **Helmet.js**: Security headers (XSS, clickjacking protection)
- **SQL Injection Protection**: Parameterized queries

### Production Security Enhancements

#### 1. Authentication & Authorization
```javascript
// JWT Token implementation
const jwt = require('jsonwebtoken');

// Middleware
const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    
    if (!token) {
        return res.sendStatus(401);
    }
    
    jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
        if (err) return res.sendStatus(403);
        req.user = user;
        next();
    });
};
```

#### 2. Data Encryption
```javascript
// Encrypt sensitive data
const crypto = require('crypto');
const algorithm = 'aes-256-gcm';

function encrypt(text, password) {
    const iv = crypto.randomBytes(16);
    const cipher = crypto.createCipher(algorithm, password);
    // ... implementation
}
```

#### 3. HTTPS/WSS Configuration
```javascript
// SSL Certificate setup
const https = require('https');
const fs = require('fs');

const options = {
    key: fs.readFileSync('path/to/private-key.pem'),
    cert: fs.readFileSync('path/to/certificate.pem')
};

const server = https.createServer(options, app);
```

### Android Security
- **Certificate Pinning**: Prevent MITM attacks
- **Root Detection**: Detect compromised devices  
- **Anti-tampering**: Protect against reverse engineering
- **Secure Storage**: Encrypt sensitive configuration

## Development Guide

### Prerequisites
```bash
# Node.js version
node --version  # >= 16.0.0

# npm version  
npm --version   # >= 8.0.0

# Dependencies
npm install
```

### Development Setup

#### 1. Environment Configuration
```bash
# Copy environment template
cp .env.example .env

# Edit configuration
PORT=3000
DB_PATH=./database.sqlite
JWT_SECRET=your-secret-key
LOG_LEVEL=debug
```

#### 2. Database Setup
```bash
# Initialize database
npm run init-db

# Verify setup
sqlite3 database.sqlite ".tables"
```

#### 3. Start Development Server
```bash
# Development mode (with nodemon)
npm run dev

# Production mode
npm start
```

### Code Structure

#### Directory Layout
```
src/
├── controllers/         # Business logic controllers
├── models/             # Database models and schemas
│   └── Database.js     # Main database class
├── routes/             # API route definitions
│   └── devices.js      # Device management routes
├── middleware/         # Custom middleware functions
├── utils/              # Utility functions
└── services/           # External service integrations

scripts/
├── init-db.js          # Database initialization
├── test-client.js      # Testing utilities
└── migrate.js          # Database migrations

android-client/         # Android app source code
├── WebSocketClient.java
├── CommandHandler.java
├── CommandResult.java
└── AndroidManifest.xml
```

#### Coding Standards

##### JavaScript/Node.js
```javascript
// Use async/await instead of callbacks
async function getData() {
    try {
        const result = await database.query();
        return result;
    } catch (error) {
        logger.error('Database error:', error);
        throw error;
    }
}

// Error handling pattern
class CustomError extends Error {
    constructor(message, code, statusCode = 500) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
    }
}

// Configuration management
const config = {
    port: process.env.PORT || 3000,
    database: {
        path: process.env.DB_PATH || './database.sqlite'
    }
};
```

##### Database Operations
```javascript
// Always use parameterized queries
const query = 'SELECT * FROM devices WHERE device_id = ?';
const result = await db.get(query, [deviceId]);

// Handle transactions
async function updateDeviceWithLog(deviceId, status, logMessage) {
    return new Promise((resolve, reject) => {
        db.serialize(() => {
            db.run('BEGIN TRANSACTION');
            
            db.run('UPDATE devices SET status = ? WHERE device_id = ?', 
                   [status, deviceId], function(err) {
                if (err) {
                    db.run('ROLLBACK');
                    reject(err);
                    return;
                }
                
                db.run('INSERT INTO activity_logs (device_id, message) VALUES (?, ?)',
                       [deviceId, logMessage], function(err) {
                    if (err) {
                        db.run('ROLLBACK');
                        reject(err);
                    } else {
                        db.run('COMMIT');
                        resolve();
                    }
                });
            });
        });
    });
}
```

### Testing

#### Unit Tests
```javascript
// test/database.test.js
const Database = require('../src/models/Database');

describe('Database Operations', () => {
    let db;
    
    beforeEach(async () => {
        db = new Database(':memory:'); // In-memory database for testing
        await db.initTables();
    });
    
    afterEach(async () => {
        await db.close();
    });
    
    test('should register device', async () => {
        const deviceInfo = {
            device_id: 'test_device',
            device_name: 'Test Device',
            device_model: 'Test Model',
            android_version: '11'
        };
        
        const result = await db.registerDevice(deviceInfo);
        expect(result.registered).toBe(true);
    });
});
```

#### Integration Tests
```bash
# Run test client
npm test

# Custom test scenarios
node scripts/test-client.js
```

### Logging

#### Winston Configuration
```javascript
const winston = require('winston');

const logger = winston.createLogger({
    level: process.env.LOG_LEVEL || 'info',
    format: winston.format.combine(
        winston.format.timestamp(),
        winston.format.errors({ stack: true }),
        winston.format.json()
    ),
    transports: [
        new winston.transports.File({ filename: 'logs/error.log', level: 'error' }),
        new winston.transports.File({ filename: 'logs/combined.log' }),
        new winston.transports.Console({
            format: winston.format.simple()
        })
    ]
});
```

## Deployment

### Production Checklist

#### Server Configuration
- [ ] Set `NODE_ENV=production`
- [ ] Configure proper database path
- [ ] Set up SSL certificates
- [ ] Configure reverse proxy (Nginx)
- [ ] Set up process manager (PM2)
- [ ] Configure logging
- [ ] Set up monitoring
- [ ] Configure backups

#### Security Hardening
- [ ] Implement authentication
- [ ] Enable HTTPS/WSS
- [ ] Configure firewall rules
- [ ] Set up intrusion detection
- [ ] Regular security updates
- [ ] Audit logging

#### Docker Deployment
```bash
# Build image
docker build -t idsiber-eye:latest .

# Run with docker-compose
docker-compose up -d

# Check logs
docker-compose logs -f idsiber-eye-server
```

#### Manual Deployment
```bash
# Server setup (Ubuntu/CentOS)
sudo apt update
sudo apt install nodejs npm nginx sqlite3

# Application deployment
git clone <repository>
cd idsiber-eye
npm ci --only=production
npm run init-db

# Process manager
npm install -g pm2
pm2 start server.js --name idsiber-eye
pm2 save
pm2 startup
```

### Monitoring

#### Health Check Endpoints
```javascript
// Health check with detailed status
app.get('/health/detailed', async (req, res) => {
    const status = {
        server: 'healthy',
        database: 'unknown',
        websocket: 'healthy',
        connectedDevices: connectedDevices.size,
        uptime: process.uptime(),
        memory: process.memoryUsage(),
        timestamp: new Date().toISOString()
    };
    
    try {
        await db.getAllDevices();
        status.database = 'healthy';
    } catch (error) {
        status.database = 'unhealthy';
        status.server = 'degraded';
    }
    
    const httpStatus = status.server === 'healthy' ? 200 : 503;
    res.status(httpStatus).json(status);
});
```

#### Log Monitoring
```bash
# PM2 monitoring
pm2 monit

# Log analysis
tail -f logs/combined.log | grep ERROR
```

## Troubleshooting

### Common Issues

#### Database Connection Errors
```bash
# Error: SQLITE_CANTOPEN
# Solution: Check file permissions
chmod 755 .
chmod 644 database.sqlite

# Error: SQLITE_BUSY
# Solution: Check for long-running queries
```

#### WebSocket Connection Issues
```javascript
// Client connection timeout
socket.timeout(5000).emit('register_device', data, (err) => {
    if (err) {
        console.error('Registration timeout');
        // Implement retry logic
    }
});
```

#### Android Permissions
```java
// Check if device admin is enabled
DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
ComponentName adminReceiver = new ComponentName(this, AdminReceiver.class);

if (!dpm.isAdminActive(adminReceiver)) {
    // Request device admin permission
    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminReceiver);
    startActivity(intent);
}
```

### Debug Mode

#### Enable Verbose Logging
```bash
# Set log level
export LOG_LEVEL=debug
npm start

# Or in .env file
LOG_LEVEL=debug
```

#### WebSocket Debug
```javascript
// Client-side debugging
const socket = io('http://localhost:3000', {
    debug: true,
    transports: ['websocket']
});

socket.on('connect', () => {
    console.log('Connected with transport:', socket.io.engine.transport.name);
});
```

### Performance Optimization

#### Database Optimization
```sql
-- Create indexes for better performance
CREATE INDEX idx_devices_device_id ON devices(device_id);
CREATE INDEX idx_activity_logs_device_id ON activity_logs(device_id);
CREATE INDEX idx_activity_logs_timestamp ON activity_logs(timestamp);

-- Cleanup old logs
DELETE FROM activity_logs WHERE timestamp < datetime('now', '-30 days');
```

#### Memory Monitoring
```javascript
// Memory usage tracking
setInterval(() => {
    const used = process.memoryUsage();
    console.log('Memory usage:');
    for (let key in used) {
        console.log(`${key}: ${Math.round(used[key] / 1024 / 1024 * 100) / 100} MB`);
    }
}, 30000);
```

---

## Contributing

### Development Workflow
1. Fork repository
2. Create feature branch: `git checkout -b feature/new-feature`
3. Make changes with proper tests
4. Update documentation
5. Submit pull request

### Code Review Checklist
- [ ] Code follows established patterns
- [ ] All tests pass
- [ ] Documentation updated
- [ ] Security considerations reviewed
- [ ] Performance impact assessed
- [ ] Error handling implemented

---

*Last updated: [Current Date]*
*Version: 1.0.0*