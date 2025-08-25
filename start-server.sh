#!/bin/bash

# IdSiber-Eye Server Startup Script
# Listen on specific IP address: 192.168.8.179

echo "🚀 Starting IdSiber-Eye Server on 192.168.8.179:3000"
echo "=================================================="

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Node.js not installed. Please install Node.js first."
    exit 1
fi

# Check if dependencies are installed
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
fi

# Initialize database if needed
if [ ! -f "database.sqlite" ]; then
    echo "🗄️ Initializing database..."
    npm run init-db
fi

# Start server
echo "🎆 Starting server..."
HOST=192.168.8.179 PORT=3000 node server.js