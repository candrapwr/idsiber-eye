package com.idsiber.eye;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class untuk menyimpan dan mendapatkan konfigurasi server
 */
public class ServerConfig {
    private static final String PREFS_NAME = "IdSiberEyeConfig";
    private static final String KEY_SERVER_IP = "server_ip";
    private static final String KEY_SERVER_PORT = "server_port";
    
    // Default values dari Constants
    private static final String DEFAULT_IP = Constants.SERVER_PUBLIC_IP;
    private static final int DEFAULT_PORT = Constants.PORT;
    
    private final SharedPreferences prefs;
    
    public ServerConfig(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Simpan konfigurasi server
     */
    public void saveServerConfig(String ip, int port) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_SERVER_IP, ip);
        editor.putInt(KEY_SERVER_PORT, port);
        editor.apply();
    }
    
    /**
     * Dapatkan server IP (return default jika belum dikonfigurasi)
     */
    public String getServerIp() {
        return prefs.getString(KEY_SERVER_IP, DEFAULT_IP);
    }
    
    /**
     * Dapatkan server port (return default jika belum dikonfigurasi)
     */
    public int getServerPort() {
        return prefs.getInt(KEY_SERVER_PORT, DEFAULT_PORT);
    }
    
    /**
     * Dapatkan URL server lengkap
     */
    public String getServerUrl() {
        return "http://" + getServerIp() + ":" + getServerPort();
    }
    
    /**
     * Reset konfigurasi ke default
     */
    public void resetToDefault() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_SERVER_IP, DEFAULT_IP);
        editor.putInt(KEY_SERVER_PORT, DEFAULT_PORT);
        editor.apply();
    }
    
    /**
     * Cek apakah konfigurasi sudah diubah dari default
     */
    public boolean isCustomConfig() {
        return !getServerIp().equals(DEFAULT_IP) || getServerPort() != DEFAULT_PORT;
    }
}