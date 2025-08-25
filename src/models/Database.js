const sqlite3 = require('sqlite3').verbose();
const path = require('path');

class Database {
    constructor() {
        const dbPath = path.join(__dirname, '../../database.sqlite');
        console.log(`📍 Database path: ${dbPath}`);
        this.db = new sqlite3.Database(dbPath);
    }

    async initTables() {
        return new Promise((resolve, reject) => {
            // Enable foreign keys
            this.db.run('PRAGMA foreign_keys = ON', (err) => {
                if (err) {
                    console.error('Error enabling foreign keys:', err);
                    reject(err);
                    return;
                }
                
                console.log('🔧 Creating tables...');
                
                // Tabel untuk device yang terdaftar
                this.db.run(`
                    CREATE TABLE IF NOT EXISTS devices (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        device_id TEXT UNIQUE NOT NULL,
                        device_name TEXT NOT NULL,
                        device_model TEXT,
                        android_version TEXT,
                        is_online BOOLEAN DEFAULT 0,
                        last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                `, (err) => {
                    if (err) {
                        console.error('❌ Error creating devices table:', err);
                        reject(err);
                        return;
                    }
                    console.log('✅ Devices table ready');
                    
                    // Tabel untuk log aktivitas
                    this.db.run(`
                        CREATE TABLE IF NOT EXISTS activity_logs (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            device_id TEXT NOT NULL,
                            action TEXT NOT NULL,
                            status TEXT NOT NULL,
                            message TEXT,
                            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (device_id) REFERENCES devices (device_id)
                        )
                    `, (err) => {
                        if (err) {
                            console.error('❌ Error creating activity_logs table:', err);
                            reject(err);
                            return;
                        }
                        console.log('✅ Activity logs table ready');
                        
                        // Tabel untuk konfigurasi device
                        this.db.run(`
                            CREATE TABLE IF NOT EXISTS device_configs (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                device_id TEXT NOT NULL,
                                config_key TEXT NOT NULL,
                                config_value TEXT,
                                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (device_id) REFERENCES devices (device_id),
                                UNIQUE(device_id, config_key)
                            )
                        `, (err) => {
                            if (err) {
                                console.error('❌ Error creating device_configs table:', err);
                                reject(err);
                            } else {
                                console.log('✅ Device configs table ready');
                                console.log('🎉 All tables created successfully!');
                                resolve();
                            }
                        });
                    });
                });
            });
        });
    }

    // Mendaftarkan device baru
    registerDevice(deviceInfo) {
        return new Promise((resolve, reject) => {
            const { device_id, device_name, device_model, android_version } = deviceInfo;
            
            this.db.run(
                `INSERT OR REPLACE INTO devices 
                 (device_id, device_name, device_model, android_version, is_online) 
                 VALUES (?, ?, ?, ?, 1)`,
                [device_id, device_name, device_model, android_version],
                function(err) {
                    if (err) {
                        reject(err);
                    } else {
                        resolve({ device_id, registered: true });
                    }
                }
            );
        });
    }

    // Update status online device
    updateDeviceStatus(deviceId, isOnline) {
        return new Promise((resolve, reject) => {
            this.db.run(
                `UPDATE devices SET is_online = ?, last_seen = CURRENT_TIMESTAMP WHERE device_id = ?`,
                [isOnline ? 1 : 0, deviceId],
                function(err) {
                    if (err) {
                        reject(err);
                    } else {
                        resolve(true);
                    }
                }
            );
        });
    }

    // Ambil semua device
    getAllDevices() {
        return new Promise((resolve, reject) => {
            this.db.all(
                `SELECT * FROM devices ORDER BY created_at DESC`,
                [],
                (err, rows) => {
                    if (err) {
                        reject(err);
                    } else {
                        resolve(rows);
                    }
                }
            );
        });
    }

    // Ambil device berdasarkan ID
    getDevice(deviceId) {
        return new Promise((resolve, reject) => {
            this.db.get(
                `SELECT * FROM devices WHERE device_id = ?`,
                [deviceId],
                (err, row) => {
                    if (err) {
                        reject(err);
                    } else {
                        resolve(row);
                    }
                }
            );
        });
    }

    // Log aktivitas
    logActivity(deviceId, action, status, message = null) {
        return new Promise((resolve, reject) => {
            this.db.run(
                `INSERT INTO activity_logs (device_id, action, status, message) VALUES (?, ?, ?, ?)`,
                [deviceId, action, status, message],
                function(err) {
                    if (err) {
                        reject(err);
                    } else {
                        resolve({ logId: this.lastID });
                    }
                }
            );
        });
    }

    // Ambil log aktivitas
    getActivityLogs(deviceId = null, limit = 100) {
        return new Promise((resolve, reject) => {
            let query = `SELECT al.*, d.device_name 
                        FROM activity_logs al 
                        LEFT JOIN devices d ON al.device_id = d.device_id`;
            let params = [];

            if (deviceId) {
                query += ` WHERE al.device_id = ?`;
                params.push(deviceId);
            }

            query += ` ORDER BY al.timestamp DESC LIMIT ?`;
            params.push(limit);

            this.db.all(query, params, (err, rows) => {
                if (err) {
                    reject(err);
                } else {
                    resolve(rows);
                }
            });
        });
    }

    // Close database connection
    close() {
        return new Promise((resolve, reject) => {
            this.db.close((err) => {
                if (err) {
                    reject(err);
                } else {
                    resolve();
                }
            });
        });
    }
}

module.exports = Database;