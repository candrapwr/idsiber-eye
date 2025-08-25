const express = require('express');

module.exports = (db, io, connectedDevices) => {
    const router = express.Router();

    // Get all devices
    router.get('/', async (req, res) => {
        try {
            const devices = await db.getAllDevices();
            
            // Add real-time connection status
            const devicesWithStatus = devices.map(device => ({
                ...device,
                is_connected: connectedDevices.has(device.device_id),
                connection_status: connectedDevices.has(device.device_id) ? 'online' : 'offline'
            }));

            res.json({
                success: true,
                devices: devicesWithStatus,
                total: devices.length,
                online: devicesWithStatus.filter(d => d.is_connected).length
            });
        } catch (error) {
            console.error('Get devices error:', error);
            res.status(500).json({
                success: false,
                message: 'Failed to get devices'
            });
        }
    });

    // Get specific device
    router.get('/:deviceId', async (req, res) => {
        try {
            const { deviceId } = req.params;
            const device = await db.getDevice(deviceId);
            
            if (!device) {
                return res.status(404).json({
                    success: false,
                    message: 'Device not found'
                });
            }

            // Add real-time status
            device.is_connected = connectedDevices.has(deviceId);
            device.connection_status = connectedDevices.has(deviceId) ? 'online' : 'offline';

            res.json({
                success: true,
                device
            });
        } catch (error) {
            console.error('Get device error:', error);
            res.status(500).json({
                success: false,
                message: 'Failed to get device'
            });
        }
    });

    // Get device activity logs
    router.get('/:deviceId/logs', async (req, res) => {
        try {
            const { deviceId } = req.params;
            const limit = parseInt(req.query.limit) || 50;
            
            const logs = await db.getActivityLogs(deviceId, limit);
            
            res.json({
                success: true,
                logs,
                device_id: deviceId,
                total: logs.length
            });
        } catch (error) {
            console.error('Get logs error:', error);
            res.status(500).json({
                success: false,
                message: 'Failed to get logs'
            });
        }
    });

    // Get all activity logs
    router.get('/logs/all', async (req, res) => {
        try {
            const limit = parseInt(req.query.limit) || 100;
            const logs = await db.getActivityLogs(null, limit);
            
            res.json({
                success: true,
                logs,
                total: logs.length
            });
        } catch (error) {
            console.error('Get all logs error:', error);
            res.status(500).json({
                success: false,
                message: 'Failed to get logs'
            });
        }
    });

    // Send command to device
    router.post('/:deviceId/command', async (req, res) => {
        try {
            const { deviceId } = req.params;
            const { action, params } = req.body;

            if (!action) {
                return res.status(400).json({
                    success: false,
                    message: 'Action is required'
                });
            }

            // Cek apakah device online
            const deviceSocket = connectedDevices.get(deviceId);
            if (!deviceSocket) {
                return res.status(404).json({
                    success: false,
                    message: 'Device is not online'
                });
            }

            // Generate command ID
            const commandId = `cmd_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

            // Send command
            deviceSocket.emit('command', {
                commandId,
                action,
                params: params || {},
                timestamp: new Date().toISOString()
            });

            // Log command
            await db.logActivity(deviceId, action, 'sent', `Command sent: ${action}`);

            res.json({
                success: true,
                commandId,
                action,
                message: `Command '${action}' sent to device ${deviceId}`,
                timestamp: new Date().toISOString()
            });

        } catch (error) {
            console.error('Send command error:', error);
            res.status(500).json({
                success: false,
                message: 'Failed to send command'
            });
        }
    });

    // Quick actions for common commands
    router.post('/:deviceId/lock', async (req, res) => {
        try {
            const { deviceId } = req.params;
            const { duration } = req.body; // durasi dalam menit
            
            const deviceSocket = connectedDevices.get(deviceId);
            if (!deviceSocket) {
                return res.status(404).json({
                    success: false,
                    message: 'Device is not online'
                });
            }

            const commandId = `lock_${Date.now()}`;
            deviceSocket.emit('command', {
                commandId,
                action: 'lock_screen',
                params: {
                    duration: duration || 60 // default 60 menit
                },
                timestamp: new Date().toISOString()
            });

            await db.logActivity(deviceId, 'lock_screen', 'sent', `Screen locked for ${duration || 60} minutes`);

            res.json({
                success: true,
                message: `Device ${deviceId} locked for ${duration || 60} minutes`
            });

        } catch (error) {
            console.error('Lock device error:', error);
            res.status(500).json({
                success: false,
                message: 'Failed to lock device'
            });
        }
    });

    router.post('/:deviceId/unlock', async (req, res) => {
        try {
            const { deviceId } = req.params;
            
            const deviceSocket = connectedDevices.get(deviceId);
            if (!deviceSocket) {
                return res.status(404).json({
                    success: false,
                    message: 'Device is not online'
                });
            }

            const commandId = `unlock_${Date.now()}`;
            deviceSocket.emit('command', {
                commandId,
                action: 'unlock_screen',
                params: {},
                timestamp: new Date().toISOString()
            });

            await db.logActivity(deviceId, 'unlock_screen', 'sent', 'Screen unlocked');

            res.json({
                success: true,
                message: `Device ${deviceId} unlocked`
            });

        } catch (error) {
            console.error('Unlock device error:', error);
            res.status(500).json({
                success: false,
                message: 'Failed to unlock device'
            });
        }
    });

    router.post('/:deviceId/reboot', async (req, res) => {
        try {
            const { deviceId } = req.params;
            
            const deviceSocket = connectedDevices.get(deviceId);
            if (!deviceSocket) {
                return res.status(404).json({
                    success: false,
                    message: 'Device is not online'
                });
            }

            const commandId = `reboot_${Date.now()}`;
            deviceSocket.emit('command', {
                commandId,
                action: 'reboot_device',
                params: {},
                timestamp: new Date().toISOString()
            });

            await db.logActivity(deviceId, 'reboot_device', 'sent', 'Reboot command sent');

            res.json({
                success: true,
                message: `Reboot command sent to device ${deviceId}`
            });

        } catch (error) {
            console.error('Reboot device error:', error);
            res.status(500).json({
                success: false,
                message: 'Failed to reboot device'
            });
        }
    });

    return router;
};
