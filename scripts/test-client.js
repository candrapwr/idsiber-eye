const io = require('socket.io-client');
const axios = require('axios');

class TestClient {
    constructor(serverUrl = 'http://localhost:3000') {
        this.serverUrl = serverUrl;
        this.deviceId = `test_device_${Date.now()}`;
        this.socket = null;
    }

    async testAPIEndpoints() {
        console.log('🧪 Testing API Endpoints...');
        
        try {
            // Test health check
            const healthResponse = await axios.get(`${this.serverUrl}/health`);
            console.log('✅ Health Check:', healthResponse.data);
            
            // Test get devices
            const devicesResponse = await axios.get(`${this.serverUrl}/api/devices`);
            console.log('✅ Get Devices:', devicesResponse.data);
            
            // Test get logs
            const logsResponse = await axios.get(`${this.serverUrl}/api/devices/logs/all?limit=10`);
            console.log('✅ Get Logs:', logsResponse.data);
            
        } catch (error) {
            console.error('❌ API Test Error:', error.message);
        }
    }

    async testWebSocketConnection() {
        console.log('🔌 Testing WebSocket Connection...');
        
        return new Promise((resolve) => {
            this.socket = io(this.serverUrl);
            
            this.socket.on('connect', () => {
                console.log('✅ WebSocket Connected');
                
                // Register device
                const deviceInfo = {
                    device_id: this.deviceId,
                    device_name: 'Test Device',
                    device_model: 'Test Model',
                    android_version: '11'
                };
                
                this.socket.emit('register_device', deviceInfo);
            });
            
            this.socket.on('registration_success', (data) => {
                console.log('✅ Device Registration Success:', data);
                resolve(true);
            });
            
            this.socket.on('registration_error', (error) => {
                console.error('❌ Registration Error:', error);
                resolve(false);
            });
            
            this.socket.on('command', (command) => {
                console.log('📨 Received Command:', command);
                
                // Send mock response
                this.socket.emit('command_response', {
                    commandId: command.commandId,
                    action: command.action,
                    success: true,
                    message: 'Command executed successfully',
                    result: 'Mock result'
                });
            });
            
            this.socket.on('disconnect', () => {
                console.log('🔌 WebSocket Disconnected');
            });
        });
    }

    async testCommands() {
        console.log('⚡ Testing Commands...');
        
        try {
            // Test lock command
            const lockResponse = await axios.post(
                `${this.serverUrl}/api/devices/${this.deviceId}/lock`,
                { duration: 5 }
            );
            console.log('✅ Lock Command:', lockResponse.data);
            
            // Wait a bit
            await new Promise(resolve => setTimeout(resolve, 1000));
            
            // Test unlock command
            const unlockResponse = await axios.post(
                `${this.serverUrl}/api/devices/${this.deviceId}/unlock`
            );
            console.log('✅ Unlock Command:', unlockResponse.data);
            
            // Test custom command
            const customResponse = await axios.post(
                `${this.serverUrl}/api/devices/${this.deviceId}/command`,
                {
                    action: 'get_device_info',
                    params: {}
                }
            );
            console.log('✅ Custom Command:', customResponse.data);
            
        } catch (error) {
            console.error('❌ Command Test Error:', error.message);
        }
    }

    async runAllTests() {
        console.log('🚀 Starting IdSiber-Eye Tests...');
        console.log('='.repeat(50));
        
        // Test API endpoints
        await this.testAPIEndpoints();
        
        console.log('\n');
        
        // Test WebSocket connection
        const wsConnected = await this.testWebSocketConnection();
        
        if (wsConnected) {
            // Wait a bit for registration to complete
            await new Promise(resolve => setTimeout(resolve, 2000));
            
            console.log('\n');
            
            // Test commands
            await this.testCommands();
        }
        
        console.log('\n');
        console.log('🏁 Tests Completed!');
        
        // Cleanup
        if (this.socket) {
            this.socket.disconnect();
        }
        
        process.exit(0);
    }
}

// Run tests if called directly
if (require.main === module) {
    const tester = new TestClient();
    tester.runAllTests();
}

module.exports = TestClient;