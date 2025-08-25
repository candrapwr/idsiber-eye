const Database = require('../src/models/Database');

async function initDatabase() {
    console.log('🔄 Initializing IdSiber-Eye database...');
    
    let db;
    try {
        db = new Database();
        
        // Initialize tables first
        await db.initTables();
        
        console.log('✅ Database initialized successfully');
        
        // Test connection by fetching data
        const devices = await db.getAllDevices();
        console.log(`📱 Found ${devices.length} registered devices`);
        
        const logs = await db.getActivityLogs(null, 10);
        console.log(`📝 Found ${logs.length} recent activity logs`);
        
        // Insert a test log entry to verify everything works
        if (devices.length === 0) {
            console.log('🧪 Creating test entries...');
            
            // Register a test device
            await db.registerDevice({
                device_id: 'test_device_init',
                device_name: 'Test Device',
                device_model: 'Test Model',
                android_version: '11'
            });
            
            // Add a test log
            await db.logActivity('test_device_init', 'init_test', 'success', 'Database initialization test');
            
            console.log('✅ Test entries created successfully');
        }
        
        console.log('🎉 Database setup completed successfully!');
        console.log('');
        console.log('You can now run:');
        console.log('  npm start    - Start the server');
        console.log('  npm test     - Test the server');
        
    } catch (error) {
        console.error('❌ Database initialization failed:', error.message);
        console.error('\nTroubleshooting:');
        console.error('1. Make sure you have write permissions in the project directory');
        console.error('2. Check if SQLite3 is properly installed: npm list sqlite3');
        console.error('3. Try deleting database.sqlite and running again');
        process.exit(1);
    } finally {
        if (db) {
            try {
                await db.close();
                console.log('🔌 Database connection closed');
            } catch (err) {
                console.error('Warning: Error closing database:', err.message);
            }
        }
    }
}

// Run if called directly
if (require.main === module) {
    initDatabase();
}

module.exports = initDatabase;