package com.stunad.questcamera;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;

public class PassthroughCameraBridge {
    private static final String TAG = "QuestCameraPlugin";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private PassthroughCameraManager manager;
    private static PassthroughCameraBridge instance;
    private Activity activity;

    public PassthroughCameraBridge(Activity activity) {
        this.activity = activity;
        this.manager = PassthroughCameraManager.getInstance(activity);
        Log.i(TAG, "PassthroughCameraBridge initialized");
    }


    public static PassthroughCameraBridge getInstance(Activity activity) {
        if (instance == null) {
            instance = new PassthroughCameraBridge(activity);
        }
        return instance;
    }
    public void Init(Activity activity) {
        if (!hasCameraPermission()) {
            Log.w(TAG, "Camera permissions not granted, requesting...");
            requestCameraPermission();
            return;
        }
        if (manager != null) {
            manager.initialize();
            Log.i(TAG, "Camera manager initialized");
        } else {
            Log.e(TAG, "Failed to initialize manager");
        }
    }

    public void OpenCamera() {
        if (!hasCameraPermission()) {
            Log.w(TAG, "Camera permissions not granted, requesting...");
            requestCameraPermission();
            return;
        }
        if (manager != null) {
            manager.openCamera();
        } else {
            Log.e(TAG, "Manager not initialized!");
        }
    }

    public byte[] CaptureImageNow() {
        if (manager != null) {
            byte[] frame = manager.getLatestFrame();
            if (frame == null) {
                Log.w(TAG, "No frame data available");
            }
            return frame;
        } else {
            Log.e(TAG, "Manager not initialized!");
            return null;
        }
    }

    public void RequestPermissions(Activity activity) {
        if (!hasCameraPermission()) {
            Log.w(TAG, "Requesting camera permissions...");
            requestCameraPermission();
        } else {
            Log.i(TAG, "Camera permissions already granted");
            if (manager != null) {
                manager.initialize();
                manager.openCamera();
            }
        }
    }

    public void CloseCamera() {
        if (manager != null) {
            manager.closeCamera();
        }
    }

    public boolean HasCameraPermissions() {
        return hasCameraPermission();
    }

    private boolean hasCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return activity.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                   activity.checkSelfPermission("horizonos.permission.HEADSET_CAMERA") == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(activity, new String[]{
                android.Manifest.permission.CAMERA,
                "horizonos.permission.HEADSET_CAMERA"
            }, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }
}