package com.idsiber.eye;

/**
 * Constants for application configuration
 */
public class Constants {
    // Server configuration
    public static final String SERVER_PUBLIC_IP = "10.88.66.40";
    public static final int PORT = 3001;
    
    // WebSocket configuration
    public static final int SOCKET_TIMEOUT = 10000;  // 10 seconds
    public static final int RECONNECTION_ATTEMPTS = 10;
    public static final int RECONNECTION_DELAY = 2000;  // 2 seconds
    
    // Heartbeat configuration
    public static final int HEARTBEAT_INTERVAL = 30000;  // 30 seconds
    
    // App version
    public static final String APP_VERSION = "1.0.1";
}
