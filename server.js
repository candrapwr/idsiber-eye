const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const Database = require('./src/models/Database');
const deviceRoutes = require('./src/routes/devices');

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
        // Security middleware
        this.app.use(helmet());
        
        // Rate limiting
        const limiter = rateLimit({
            windowMs: 15 * 60 * 1000, // 15 minutes
            max: 100 // limit each IP to 100 requests per windowMs
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

    async start(port = 3000, host = '0.0.0.0') {
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
            console.log(`🔗 API endpoint: http://${host === '0.0.0.0' ? '10.88.66.40' : host}:${port}`);
            console.log(`📄 Health check: http://${host === '0.0.0.0' ? '10.88.66.40' : host}:${port}/health`);
            console.log(`📱 Android client: http://10.88.66.40:${port}`);
            console.log('=' .repeat(40));
            console.log('');
        });
    }
}

// Start server
if (require.main === module) {
    const server = new IdSiberEyeServer();
    server.start(process.env.PORT || 3001).catch(error => {
        console.error('❌ Failed to start server:', error);
        process.exit(1);
    });
}

module.exports = IdSiberEyeServer;