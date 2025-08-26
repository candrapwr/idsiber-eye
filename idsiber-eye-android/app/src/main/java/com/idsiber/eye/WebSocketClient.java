package com.idsiber.eye;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

public class WebSocketClient {
    private static final String TAG = "WebSocketClient";
    private static final String SERVER_URL = "http://10.88.66.40:3001"; // SERVER IP
    
    private Socket socket;
    private Context context;
    private CommandHandler commandHandler;
    private StatusCallback statusCallback;
    private boolean isConnected = false;
    
    public interface StatusCallback {
        void onStatusChange(String status);
        void onError(String error);
    }
    
    public WebSocketClient(Context context, StatusCallback callback) {
        this.context = context;
        this.statusCallback = callback;
        this.commandHandler = new CommandHandler(context);
        initSocket();
    }
    
    private void initSocket() {
        try {
            IO.Options options = new IO.Options();
            options.timeout = 10000; // 10 seconds timeout
            options.reconnection = true;
            options.reconnectionAttempts = 10; // More attempts
            options.reconnectionDelay = 2000; // 2 second delay
            options.forceNew = true;
            options.transports = new String[]{"websocket", "polling"}; // Try both transport methods
            
            Log.d(TAG, "Attempting to connect to: " + SERVER_URL);
            socket = IO.socket(SERVER_URL, options);
            setupSocketListeners();
            
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket initialization error", e);
            if (statusCallback != null) {
                statusCallback.onError("Failed to initialize: " + e.getMessage());
            }
        }
    }
    
    private void setupSocketListeners() {
        // Connection events
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Connected to server");
                isConnected = true;
                if (statusCallback != null) {
                    statusCallback.onStatusChange("Connected - Registering device...");
                }
                registerDevice();
            }
        });
        
        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Disconnected from server");
                isConnected = false;
                if (statusCallback != null) {
                    statusCallback.onStatusChange("Disconnected");
                }
            }
        });
        
        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String errorMsg = "Unknown connection error";
                if (args.length > 0 && args[0] != null) {
                    errorMsg = args[0].toString();
                }
                Log.e(TAG, "Connection error: " + errorMsg);
                isConnected = false;
                if (statusCallback != null) {
                    statusCallback.onError("Connection failed: " + errorMsg);
                    statusCallback.onStatusChange("Connection Failed - Check Network");
                }
            }
        });
        
        // Registration events
        socket.on("registration_success", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Device registered successfully");
                if (statusCallback != null) {
                    statusCallback.onStatusChange("Connected & Registered ✓");
                }
            }
        });
        
        socket.on("registration_error", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e(TAG, "Registration failed");
                if (statusCallback != null) {
                    statusCallback.onError("Registration failed");
                    statusCallback.onStatusChange("Registration Failed");
                }
            }
        });
        
        // Command handling
        socket.on("command", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject command = (JSONObject) args[0];
                    Log.d(TAG, "Received command: " + command.toString());
                    handleCommand(command);
                } catch (Exception e) {
                    Log.e(TAG, "Error handling command", e);
                }
            }
        });
        
        // Heartbeat
        socket.on("heartbeat_response", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Heartbeat received");
            }
        });
    }
    
    private void registerDevice() {
        try {
            JSONObject deviceInfo = new JSONObject();
            deviceInfo.put("device_id", getDeviceId());
            deviceInfo.put("device_name", Build.MODEL);
            deviceInfo.put("device_model", Build.DEVICE);
            deviceInfo.put("android_version", Build.VERSION.RELEASE);
            
            socket.emit("register_device", deviceInfo);
            Log.d(TAG, "Sending device registration: " + deviceInfo.toString());
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating device info", e);
            if (statusCallback != null) {
                statusCallback.onError("Failed to create device info");
            }
        }
    }
    
    private String getDeviceId() {
        return Settings.Secure.getString(context.getContentResolver(), 
                                       Settings.Secure.ANDROID_ID);
    }
    
    private void handleCommand(JSONObject command) {
        try {
            String commandId = command.getString("commandId");
            String action = command.getString("action");
            JSONObject params = command.optJSONObject("params");
            
            Log.d(TAG, "Executing command: " + action);
            
            // Execute command
            CommandResult result = commandHandler.executeCommand(action, params);
            
            // Send response
            sendCommandResponse(commandId, action, result);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing command", e);
        }
    }
    
    private void sendCommandResponse(String commandId, String action, CommandResult result) {
        try {
            JSONObject response = new JSONObject();
            response.put("commandId", commandId);
            response.put("action", action);
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("result", result.getData());
            
            socket.emit("command_response", response);
            Log.d(TAG, "Sent command response: " + response.toString());
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating command response", e);
        }
    }
    
    public void connect() {
        if (socket != null && !socket.connected()) {
            socket.connect();
            if (statusCallback != null) {
                statusCallback.onStatusChange("Connecting...");
            }
        }
    }
    
    public void disconnect() {
        if (socket != null && socket.connected()) {
            socket.disconnect();
        }
        isConnected = false;
    }
    
    public boolean isConnected() {
        return socket != null && socket.connected() && isConnected;
    }
    
    public void sendHeartbeat() {
        if (socket != null && socket.connected()) {
            socket.emit("heartbeat");
        }
    }
    
    public void sendStatusUpdate(JSONObject status) {
        if (socket != null && socket.connected()) {
            socket.emit("status_update", status);
        }
    }
}