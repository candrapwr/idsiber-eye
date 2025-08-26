const express = require('express');
const path = require('path');

module.exports = () => {
    const router = express.Router();

    // Serve portal.html as main page
    router.get('/', (req, res) => {
        res.sendFile(path.join(__dirname, '../web/portal.html'));
    });

    // Serve static files from web directory
    router.use(express.static(path.join(__dirname, '../web')));

    // Health check endpoint for web portal
    router.get('/health', (req, res) => {
        res.json({
            success: true,
            message: 'Web portal is running',
            timestamp: new Date().toISOString()
        });
    });

    return router;
};