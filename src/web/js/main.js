const SERVER_URL = window.location.origin;
// Versi aplikasi
const APP_VERSION = '1.0.1';
let selectedDeviceId = null;
let socket = null;

$(document).ready(function() {
    // Tab navigation
    $('.tab').click(function() {
        $('.tab').removeClass('active');
        $(this).addClass('active');
        
        const tabId = $(this).data('tab');
        $('.tab-content').removeClass('active');
        $(`#tab-${tabId}`).addClass('active');
    });

    // Volume slider
    $('#volume-level').on('input', function() {
        $('#volume-value').text($(this).val() + '%');
    });

    // Brightness slider
    $('#brightness-level').on('input', function() {
        $('#brightness-value').text($(this).val() + '%');
    });
    
    // Load devices
    loadDevices();
    
    // Auto refresh every 30 seconds
    setInterval(loadDevices, 30000);
});

function loadDevices() {
    $.ajax({
        url: `${SERVER_URL}/api/devices`,
        method: 'GET',
        success: function(response) {
            if (response.success) {
                updateDeviceList(response.devices);
                $('#connected-devices').text(response.online || 0);
                $('#total-devices').text(response.total || 0);
            }
        },
        error: function() {
            $('#device-list').html('<li class="device-item">Error loading devices</li>');
        }
    });
}

function updateDeviceList(devices) {
    const deviceList = $('#device-list');
    deviceList.empty();

    if (devices.length === 0) {
        deviceList.append('<li class="device-item">Tidak ada perangkat terdaftar</li>');
        return;
    }

    devices.forEach(device => {
        const statusClass = device.is_online ? 'online' : 'offline';
        const statusText = device.is_online ? 'Online' : 'Offline';
        
        deviceList.append(`
            <li class="device-item ${statusClass} ${selectedDeviceId === device.device_id ? 'active' : ''}" 
                 onclick="selectDevice('${device.device_id}')">
                <strong>${device.device_name}</strong><br>
                <small>${device.device_model} • ${statusText}</small><br>
                <small>ID: ${device.device_id}</small>
            </li>
        `);
    });
}

function selectDevice(deviceId) {
    selectedDeviceId = deviceId;
    $('.device-item').removeClass('active');
    $(`.device-item:contains('${deviceId}')`).addClass('active');
    
    // Update response area
    $('#response-area').html(`
        <div class="response-success">
            ➤ Perangkat dipilih: ${deviceId}
        </div>
    `);
}

function sendCommand(action, params = {}) {
    if (!selectedDeviceId) {
        showResponse('error', 'Pilih perangkat terlebih dahulu!');
        return;
    }

    const commandId = 'cmd_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    
    showResponse('info', `Mengirim command: ${action}... <span class="loading"></span>`);

    $.ajax({
        url: `${SERVER_URL}/api/devices/${selectedDeviceId}/command`,
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            action: action,
            params: params
        }),
        success: function(response) {
            if (response.success) {
                showResponse('success', 
                    `✅ Command berhasil!<br>
                     Action: ${action}<br>
                     Message: ${response.message}<br>
                     Result: ${response.result ? JSON.stringify(JSON.parse(response.result), null, 2) : 'No data'}`
                );
            } else {
                showResponse('error', 
                    `❌ Command gagal!<br>
                     Action: ${action}<br>
                     Error: ${response.message}`
                );
            }
        },
        error: function(xhr) {
            showResponse('error', 
                `❌ Server error!<br>
                 Status: ${xhr.status}<br>
                 Response: ${xhr.responseText || 'No response'}`
            );
        }
    });
}

function showResponse(type, message) {
    const responseArea = $('#response-area');
    const responseClass = type === 'success' ? 'response-success' : 
                        type === 'error' ? 'response-error' : 'response-info';
    
    responseArea.html(`<div class="${responseClass}">${message}</div>`);
    responseArea.scrollTop(responseArea[0].scrollHeight);
}

// Socket.IO connection untuk real-time updates
function connectSocketIO() {
    // Gunakan URL yang sama dengan halaman portal
    socket = io({transports: ['websocket', 'polling']});

    socket.on('connect', function() {
        console.log('Socket.IO connected');
        showResponse('info', '✅ Socket.IO connected untuk real-time updates');
    });

    socket.on('real_time_update', function(data) {
        console.log('Real-time update:', data);
        
        if (data.type === 'command_response') {
            showResponse(data.success ? 'success' : 'error', 
                `🎯 REAL-TIME RESPONSE from ${data.deviceId}:<br>
                 Action: <strong>${data.action}</strong><br>
                 Status: ${data.success ? '✅ Success' : '❌ Failed'}<br>
                 Message: ${data.message}<br>
                 ${data.result ? 'Result: ' + JSON.stringify(JSON.parse(data.result), null, 2) : ''}`
            );
        }
        
        if (data.type === 'device_status') {
            // Auto refresh device list ketika ada perubahan status
            loadDevices();
        }
    });

    socket.on('disconnect', function() {
        console.log('Socket.IO disconnected');
        showResponse('info', 'Socket.IO disconnected');
    });

    socket.on('connect_error', function(error) {
        console.error('Socket.IO connection error:', error);
        showResponse('error', 'Socket.IO connection error: ' + error.message);
    });

    socket.on('error', function(error) {
        console.error('Socket.IO error:', error);
        showResponse('error', 'Socket.IO error: ' + error.message);
    });
}

// Connect Socket.IO saat halaman loaded
connectSocketIO();