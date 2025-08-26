package com.idsiber.eye.handlers;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.idsiber.eye.CommandResult;
import com.idsiber.eye.IdSiberDeviceAdminReceiver;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Handler untuk manajemen aplikasi - install, uninstall, enable, disable, block, dll
 */
public class AppManagementHandler {
    private static final String TAG = "AppManagementHandler";
    private Context context;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdminReceiver;

    public AppManagementHandler(Context context) {
        this.context = context;
        this.devicePolicyManager = (DevicePolicyManager) 
            context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.deviceAdminReceiver = new ComponentName(context, IdSiberDeviceAdminReceiver.class);
    }

    public CommandResult getInstalledApps() {
        try {
            PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            
            JSONArray appList = new JSONArray();
            int userApps = 0;
            
            for (ApplicationInfo app : apps) {
                try {
                    JSONObject appInfo = new JSONObject();
                    appInfo.put("package_name", app.packageName);
                    appInfo.put("app_name", pm.getApplicationLabel(app).toString());
                    appInfo.put("is_system", (app.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
                    appInfo.put("enabled", app.enabled);
                    appInfo.put("uid", app.uid);
                    appInfo.put("target_sdk", app.targetSdkVersion);
                    
                    try {
                        String versionName = pm.getPackageInfo(app.packageName, 0).versionName;
                        int versionCode = pm.getPackageInfo(app.packageName, 0).versionCode;
                        appInfo.put("version_name", versionName != null ? versionName : "Unknown");
                        appInfo.put("version_code", versionCode);
                    } catch (Exception e) {
                        appInfo.put("version_name", "Unknown");
                        appInfo.put("version_code", 0);
                    }
                    
                    appList.put(appInfo);
                    
                    if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        userApps++;
                    }
                } catch (Exception appError) {
                    Log.w(TAG, "Error processing app: " + app.packageName);
                }
            }
            
            JSONObject result = new JSONObject();
            result.put("apps", appList);
            result.put("total_apps", appList.length());
            result.put("user_apps", userApps);
            result.put("system_apps", appList.length() - userApps);
            
            return new CommandResult(true, "Found " + appList.length() + " installed apps", result.toString());
            
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get installed apps: " + e.getMessage(), null);
        }
    }

    public CommandResult blockApp(JSONObject params) {
        try {
            String packageName = params.getString("package_name");
            
            if (devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
                try {
                    // Try to disable the application
                    devicePolicyManager.setApplicationHidden(deviceAdminReceiver, packageName, true);
                    return new CommandResult(true, "App blocked: " + packageName, null);
                } catch (Exception e) {
                    Log.w(TAG, "Device admin block failed, app may require device owner permissions");
                    return new CommandResult(false, "App blocking requires device owner permissions", null);
                }
            } else {
                return new CommandResult(false, "Device admin permission required", null);
            }
        } catch (Exception e) {
            return new CommandResult(false, "Failed to block app: " + e.getMessage(), null);
        }
    }
    
    public CommandResult unblockApp(JSONObject params) {
        try {
            String packageName = params.getString("package_name");
            
            if (devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
                try {
                    // Try to enable the application
                    devicePolicyManager.setApplicationHidden(deviceAdminReceiver, packageName, false);
                    return new CommandResult(true, "App unblocked: " + packageName, null);
                } catch (Exception e) {
                    Log.w(TAG, "Device admin unblock failed");
                    return new CommandResult(false, "App unblocking requires device owner permissions", null);
                }
            } else {
                return new CommandResult(false, "Device admin permission required", null);
            }
        } catch (Exception e) {
            return new CommandResult(false, "Failed to unblock app: " + e.getMessage(), null);
        }
    }

    public CommandResult killApp(JSONObject params) {
        try {
            String packageName = params.getString("package_name");
            ActivityManager activityManager = (ActivityManager) 
                context.getSystemService(Context.ACTIVITY_SERVICE);
            
            if (activityManager == null) {
                return new CommandResult(false, "ActivityManager not available", null);
            }
            
            activityManager.killBackgroundProcesses(packageName);
            Log.d(TAG, "Killed app: " + packageName);
            return new CommandResult(true, "App terminated: " + packageName, null);
            
        } catch (Exception e) {
            return new CommandResult(false, "Failed to kill app: " + e.getMessage(), null);
        }
    }

    public CommandResult forceStopApp(JSONObject params) {
        try {
            String packageName = params.getString("package_name");
            
            if (devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
                try {
                    // Use device admin to force stop
                    ActivityManager activityManager = (ActivityManager) 
                        context.getSystemService(Context.ACTIVITY_SERVICE);
                    
                    if (activityManager != null) {
                        activityManager.killBackgroundProcesses(packageName);
                        
                        // Also try to use device policy manager
                        devicePolicyManager.clearPackagePersistentPreferredActivities(
                            deviceAdminReceiver, packageName);
                        
                        return new CommandResult(true, "App force stopped: " + packageName, null);
                    } else {
                        return new CommandResult(false, "ActivityManager not available", null);
                    }
                } catch (Exception e) {
                    return new CommandResult(false, "Force stop requires system permissions", null);
                }
            } else {
                return new CommandResult(false, "Device admin permission required", null);
            }
        } catch (Exception e) {
            return new CommandResult(false, "Failed to force stop app: " + e.getMessage(), null);
        }
    }

    public CommandResult disableApp(JSONObject params) {
        try {
            String packageName = params.getString("package_name");
            
            if (devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
                try {
                    PackageManager pm = context.getPackageManager();
                    pm.setApplicationEnabledSetting(packageName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                    
                    return new CommandResult(true, "App disabled: " + packageName, null);
                } catch (Exception e) {
                    return new CommandResult(false, "App disable requires system permissions", null);
                }
            } else {
                return new CommandResult(false, "Device admin permission required", null);
            }
        } catch (Exception e) {
            return new CommandResult(false, "Failed to disable app: " + e.getMessage(), null);
        }
    }

    public CommandResult enableApp(JSONObject params) {
        try {
            String packageName = params.getString("package_name");
            
            if (devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
                try {
                    PackageManager pm = context.getPackageManager();
                    pm.setApplicationEnabledSetting(packageName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
                    
                    return new CommandResult(true, "App enabled: " + packageName, null);
                } catch (Exception e) {
                    return new CommandResult(false, "App enable requires system permissions", null);
                }
            } else {
                return new CommandResult(false, "Device admin permission required", null);
            }
        } catch (Exception e) {
            return new CommandResult(false, "Failed to enable app: " + e.getMessage(), null);
        }
    }

    public CommandResult clearAppData(JSONObject params) {
        try {
            String packageName = params.getString("package_name");
            
            if (devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
                try {
                    // This requires device owner permissions
                    return new CommandResult(false, "Clear app data requires device owner permissions", null);
                } catch (Exception e) {
                    return new CommandResult(false, "Clear app data requires system permissions", null);
                }
            } else {
                return new CommandResult(false, "Device admin permission required", null);
            }
        } catch (Exception e) {
            return new CommandResult(false, "Failed to clear app data: " + e.getMessage(), null);
        }
    }

    public CommandResult wipeDevice(JSONObject params) {
        try {
            if (!devicePolicyManager.isAdminActive(deviceAdminReceiver)) {
                return new CommandResult(false, "Device admin permission required", null);
            }
            
            boolean wipeExternalStorage = params.optBoolean("wipe_external", false);
            
            int flags = 0;
            if (wipeExternalStorage) {
                flags = DevicePolicyManager.WIPE_EXTERNAL_STORAGE;
            }
            
            // Warning: This will factory reset the device!
            devicePolicyManager.wipeData(flags);
            
            return new CommandResult(true, "Device wipe initiated", null);
        } catch (Exception e) {
            return new CommandResult(false, "Failed to wipe device: " + e.getMessage(), null);
        }
    }

    public CommandResult getAppInfo(JSONObject params) {
        try {
            String packageName = params.getString("package_name");
            PackageManager pm = context.getPackageManager();
            
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                
                JSONObject result = new JSONObject();
                result.put("package_name", appInfo.packageName);
                result.put("app_name", pm.getApplicationLabel(appInfo).toString());
                result.put("is_system", (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
                result.put("enabled", appInfo.enabled);
                result.put("uid", appInfo.uid);
                result.put("target_sdk", appInfo.targetSdkVersion);
                result.put("data_dir", appInfo.dataDir);
                result.put("source_dir", appInfo.sourceDir);
                
                try {
                    android.content.pm.PackageInfo pkgInfo = pm.getPackageInfo(packageName, 0);
                    result.put("version_name", pkgInfo.versionName != null ? pkgInfo.versionName : "Unknown");
                    result.put("version_code", pkgInfo.versionCode);
                    result.put("first_install_time", pkgInfo.firstInstallTime);
                    result.put("last_update_time", pkgInfo.lastUpdateTime);
                } catch (Exception e) {
                    result.put("version_name", "Unknown");
                    result.put("version_code", 0);
                }
                
                return new CommandResult(true, "App info retrieved", result.toString());
            } catch (PackageManager.NameNotFoundException e) {
                return new CommandResult(false, "App not found: " + packageName, null);
            }
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get app info: " + e.getMessage(), null);
        }
    }
}
