#!/bin/bash

# Navigasi ke direktori proyek
cd "$(dirname "$0")"

# Instal dependensi
echo "Installing dependencies..."
npm install

# Restart server
echo "Restarting server..."
npm run dev