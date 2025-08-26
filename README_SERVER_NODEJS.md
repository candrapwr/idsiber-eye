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

### Services Required

#### 1. WebSocketService
- Maintains persistent connection to server
- Handles reconnection logic
- Runs in foreground with notification

#### 2. DeviceAdminReceiver
- Enables lock screen functionality and other command
- Handles device admin events
- Required for system-level controls

#### 3. AccessibilityService
- App blocking capabilities
- UI interaction automation
- Process monitoring

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

```


*Last updated: [Current Date]*
*Version: 1.0.0*