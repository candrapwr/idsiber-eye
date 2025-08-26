const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const path = require('path');
const dotenv = require('dotenv');
const Database = require('./src/models/Database');
const deviceRoutes = require('./src/routes/devices');
const webRoutes = require('./src/routes/web');

// Load environment variables
dotenv.config();

// Config constants
const PORT = process.env.PORT || 3001;
const HOST = process.env.HOST || '0.0.0.0';
const SERVER_PUBLIC_IP = process.env.SERVER_PUBLIC_IP || 'localhost';
const RATE_LIMIT_WINDOW_MS = parseInt(process.env.RATE_LIMIT_WINDOW_MS) || 15 * 60 * 1000; // 15 minutes
const RATE_LIMIT_MAX_REQUESTS = parseInt(process.env.RATE_LIMIT_MAX_REQUESTS) || 100;

class IdSiberEyeServer {
    constructor() {
        this.app = express();
        this.server = http.createServer(this.app);
        this.io = socketIo(this.server, {
            cors: {
                origin: "*",
                methods: ["GET", "POST"]
            }
        });
        this.db = new Database();
        this.connectedDevices = new Map(); // Map untuk track device yang terkoneksi
        this.isReady = false;
    }
    
    async init() {
        try {
            console.log('🔧 Initializing server...');
            
            // Initialize database tables
            await this.db.initTables();
            
            this.initMiddleware();
            this.initRoutes();
            this.initWebSocket();
            
            this.isReady = true;
            console.log('✅ Server initialization complete');
            
        } catch (error) {
            console.error('❌ Server initialization failed:', error);
            throw error;
        }
    }

    initMiddleware() {
        // Security middleware - Disabled for development
        // this.app.use(helmet());
        
        // Rate limiting
        const limiter = rateLimit({
            windowMs: RATE_LIMIT_WINDOW_MS, // from env or default 15 minutes
            max: RATE_LIMIT_MAX_REQUESTS // limit each IP to configured requests per windowMs
        });
        this.app.use(limiter);

        // CORS
        this.app.use(cors());
        
        // Body parser
        this.app.use(express.json());
        this.app.use(express.urlencoded({ extended: true }));

        // Logging middleware
        this.app.use((req, res, next) => {
            console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
            next();
        });
    }

    initRoutes() {
        // Health check
        this.app.get('/health', (req, res) => {
            res.json({ 
                status: 'ok', 
                timestamp: new Date().toISOString(),
                connectedDevices: this.connectedDevices.size
            });
        });

        // Device routes
        this.app.use('/api/devices', deviceRoutes(this.db, this.io, this.connectedDevices));

        // Notification routes
        this.app.get('/api/notifications', async (req, res) => {
            try {
                const { limit = 100, package_name, device_id } = req.query;
                
                let notifications;
                if (device_id) {
                    // Get notifications for specific device
                    notifications = await this.db.getDeviceNotifications(device_id, parseInt(limit));
                } else if (package_name) {
                    // Get notifications for specific package/app
                    notifications = await this.db.getNotificationsByPackage(package_name, parseInt(limit));
                } else {
                    // Get all notifications
                    notifications = await this.db.getAllNotifications(parseInt(limit));
                }
                
                res.json({
                    success: true,
                    count: notifications.length,
                    notifications: notifications
                });
            } catch (error) {
                console.error('Error getting notifications:', error);
                res.status(500).json({
                    success: false,
                    message: 'Failed to get notifications'
                });
            }
        });
        
        this.app.delete('/api/notifications/:id', async (req, res) => {
            try {
                const { id } = req.params;
                const result = await this.db.deleteNotification(id);
                
                res.json({
                    success: true,
                    deleted: result.deleted
                });
            } catch (error) {
                console.error('Error deleting notification:', error);
                res.status(500).json({
                    success: false,
                    message: 'Failed to delete notification'
                });
            }
        });
        
        this.app.delete('/api/notifications/device/:deviceId', async (req, res) => {
            try {
                const { deviceId } = req.params;
                const result = await this.db.clearDeviceNotifications(deviceId);
                
                res.json({
                    success: true,
                    deleted: result.deleted
                });
            } catch (error) {
                console.error('Error clearing device notifications:', error);
                res.status(500).json({
                    success: false,
                    message: 'Failed to clear device notifications'
                });
            }
        });

        // Web portal routes
        this.app.use('/portal', webRoutes());
        this.app.use('/', webRoutes());

        // Command routes
        this.app.post('/api/command/:deviceId', async (req, res) => {
            try {
                const { deviceId } = req.params;
                const { action, params } = req.body;

                // Cek apakah device online
                const deviceSocket = this.connectedDevices.get(deviceId);
                if (!deviceSocket) {
                    return res.status(404).json({
                        success: false,
                        message: 'Device not online'
                    });
                }

                // Kirim command ke device
                const commandId = Date.now().toString();
                deviceSocket.emit('command', {
                    commandId,
                    action,
                    params: params || {}
                });

                // Log command
                await this.db.logActivity(deviceId, action, 'sent', `Command sent: ${action}`);

                res.json({
                    success: true,
                    commandId,
                    message: `Command ${action} sent to device ${deviceId}`
                });

            } catch (error) {
                console.error('Command error:', error);
                res.status(500).json({
                    success: false,
                    message: 'Internal server error'
                });
            }
        });
    }

    initWebSocket() {
        this.io.on('connection', (socket) => {
            console.log(`Client connected: ${socket.id}`);

            // Device registration
            socket.on('register_device', async (deviceInfo) => {
                try {
                    console.log('Device registering:', deviceInfo);
                    
                    // Register di database
                    await this.db.registerDevice(deviceInfo);
                    
                    // Update status online
                    await this.db.updateDeviceStatus(deviceInfo.device_id, true);
                    
                    // Simpan koneksi
                    this.connectedDevices.set(deviceInfo.device_id, socket);
                    socket.deviceId = deviceInfo.device_id;
                    
                    // Log aktivitas
                    await this.db.logActivity(deviceInfo.device_id, 'connect', 'success', 'Device connected');
                    
                    socket.emit('registration_success', {
                        message: 'Device registered successfully',
                        deviceId: deviceInfo.device_id
                    });

                    console.log(`Device registered: ${deviceInfo.device_id} - ${deviceInfo.device_name}`);
                    
                } catch (error) {
                    console.error('Registration error:', error);
                    socket.emit('registration_error', {
                        message: 'Registration failed'
                    });
                }
            });

            // Command response dari device
            socket.on('command_response', async (data) => {
                try {
                    const { commandId, action, success, message, result } = data;
                    
                    if (socket.deviceId) {
                        // Log hasil command
                        await this.db.logActivity(
                            socket.deviceId, 
                            action, 
                            success ? 'success' : 'failed', 
                            message || result
                        );
                        
                        // ✅ BROADCAST REAL-TIME RESPONSE KE SEMUA CLIENT
                        this.io.emit('real_time_update', {
                            type: 'command_response',
                            deviceId: socket.deviceId,
                            action: action,
                            success: success,
                            message: message,
                            result: result,
                            timestamp: new Date().toISOString()
                        });
                    }
                    
                    console.log(`Command response from ${socket.deviceId}:`, data);
                    
                } catch (error) {
                    console.error('Command response error:', error);
                }
            });

            // Device status update
            socket.on('status_update', async (statusData) => {
                try {
                    if (socket.deviceId) {
                        // Log status update
                        await this.db.logActivity(
                            socket.deviceId, 
                            'status_update', 
                            'info', 
                            JSON.stringify(statusData)
                        );
                    }
                } catch (error) {
                    console.error('Status update error:', error);
                }
            });
            
            // Notification event
            socket.on('notification_event', async (notificationData) => {
                try {
                    if (socket.deviceId || notificationData.device_id) {
                        const deviceId = socket.deviceId || notificationData.device_id;
                        
                        // Save notification to database
                        const result = await this.db.saveNotification(
                            deviceId,
                            notificationData.notification_data
                        );
                        
                        console.log(`Notification saved for device ${deviceId}:`, 
                            notificationData.notification_data.package_name);
                        
                        // Broadcast notification to all clients
                        this.io.emit('real_time_update', {
                            type: 'notification',
                            deviceId: deviceId,
                            notification: notificationData.notification_data,
                            timestamp: new Date().toISOString()
                        });
                    }
                } catch (error) {
                    console.error('Notification handling error:', error);
                }
            });

            // Heartbeat untuk menjaga koneksi
            socket.on('heartbeat', () => {
                socket.emit('heartbeat_response', { timestamp: Date.now() });
            });

            // Disconnect
            socket.on('disconnect', async () => {
                console.log(`Client disconnected: ${socket.id}`);
                
                if (socket.deviceId) {
                    try {
                        // Update status offline
                        await this.db.updateDeviceStatus(socket.deviceId, false);
                        
                        // Remove dari connected devices
                        this.connectedDevices.delete(socket.deviceId);
                        
                        // Log disconnect
                        await this.db.logActivity(socket.deviceId, 'disconnect', 'info', 'Device disconnected');
                        
                        console.log(`Device disconnected: ${socket.deviceId}`);
                    } catch (error) {
                        console.error('Disconnect handling error:', error);
                    }
                }
            });
        });
    }

    async start(port = PORT, host = HOST) {
        if (!this.isReady) {
            await this.init();
        }
        
        this.server.listen(port, host, () => {
            console.log('');
            console.log('🎆 IdSiber-Eye Server Started!');
            console.log('=' .repeat(40));
            console.log(`🚀 Server running on ${host}:${port}`);
            console.log(`📡 WebSocket server ready`);
            console.log(`💾 Database initialized`);
            console.log(`🔗 API endpoint: http://${SERVER_PUBLIC_IP}:${port}`);
            console.log(`📄 Health check: http://${SERVER_PUBLIC_IP}:${port}/health`);
            console.log(`📱 Android client connection: http://${SERVER_PUBLIC_IP}:${port}`);
            console.log('=' .repeat(40));
            console.log('');
        });
    }
}

// Start server
if (require.main === module) {
    const server = new IdSiberEyeServer();
    server.start().catch(error => {
        console.error('❌ Failed to start server:', error);
        process.exit(1);
    });
}

module.exports = IdSiberEyeServer;