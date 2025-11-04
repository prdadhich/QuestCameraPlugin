package com.stunad.questcamera;

import android.content.Context;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import java.util.Arrays;
import android.graphics.ImageFormat;
import java.nio.ByteBuffer;
import android.hardware.camera2.params.StreamConfigurationMap; 

public class PassthroughCameraManager {
    private static final String TAG = "QuestCameraPlugin";
    private static PassthroughCameraManager instance;

    private Context context;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private String selectedCameraId;

    private byte[] latestFrameData;
    private int frameWidth = 320;
    private int frameHeight = 240; // Supported resolution
    private boolean isCapturing = false;

    private static native void onFrameAvailable(byte[] data, int width, int height);

    private PassthroughCameraManager(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public static PassthroughCameraManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new PassthroughCameraManager(ctx);
            Log.d(TAG, "PassthroughCameraManager instance created");
        }
        return instance;
    }

    public void initialize() {
        startBackgroundThread();
        Log.d(TAG, "Camera manager initialized with background thread");
    }

    public void openCamera() {
        if (isCapturing) {
            Log.w(TAG, "Camera already open");
            return;
        }

        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            Log.d(TAG, "Found " + cameraIds.length + " cameras: " + Arrays.toString(cameraIds));

            // Prioritize Quest passthrough cameras (ID 50, 51)
            selectedCameraId = null;
            for (String id : cameraIds) {
                if (id.equals("50") || id.equals("51")) {
                    CameraCharacteristics chars = cameraManager.getCameraCharacteristics(id);
                    Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                    Log.d(TAG, "Camera ID " + id + " facing: " + (facing == CameraCharacteristics.LENS_FACING_EXTERNAL ? "EXTERNAL" : "UNKNOWN"));
                    selectedCameraId = id;
                    break;
                }
            }

            // Fallback to external or first available
            if (selectedCameraId == null) {
                for (String id : cameraIds) {
                    CameraCharacteristics chars = cameraManager.getCameraCharacteristics(id);
                    Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                        selectedCameraId = id;
                        Log.d(TAG, "Selected EXTERNAL camera ID: " + id);
                        break;
                    }
                }
                if (selectedCameraId == null && cameraIds.length > 0) {
                    selectedCameraId = cameraIds[0];
                    Log.d(TAG, "Fallback to camera ID: " + selectedCameraId);
                }
            }

            if (selectedCameraId == null) {
                Log.e(TAG, "No cameras found");
                return;
            }

            // Verify supported resolutions
            CameraCharacteristics chars = cameraManager.getCameraCharacteristics(selectedCameraId);
            StreamConfigurationMap map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = map.getOutputSizes(ImageFormat.YUV_420_888);
            Log.d(TAG, "Supported YUV sizes for camera " + selectedCameraId + ": " + Arrays.toString(sizes));
            boolean resolutionSupported = false;
            for (Size size : sizes) {
                if (size.getWidth() == frameWidth && size.getHeight() == frameHeight) {
                    resolutionSupported = true;
                    break;
                }
            }
            if (!resolutionSupported) {
                Log.w(TAG, "Requested resolution " + frameWidth + "x" + frameHeight + " not supported, falling back to 320x240");
                frameWidth = 320;
                frameHeight = 240;
            }

            // Setup ImageReader
            imageReader = ImageReader.newInstance(frameWidth, frameHeight, ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    processImage(image);
                    image.close();
                }
            }, backgroundHandler);

            cameraManager.openCamera(selectedCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    createCaptureSession();
                    Log.i(TAG, "Camera opened: " + selectedCameraId);
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    Log.w(TAG, "Camera disconnected");
                    closeCamera();
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    Log.e(TAG, "Camera error: " + error);
                    closeCamera();
                }
            }, backgroundHandler);

            isCapturing = true;
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to open camera", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Camera permission denied", e);
        }
    }

    private void createCaptureSession() {
        try {
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()),
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        captureSession = session;
                        startCapture();
                        Log.i(TAG, "Capture session configured");
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {
                        Log.e(TAG, "Capture session configuration failed");
                    }
                }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to create capture session", e);
        }
    }

    private void startCapture() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(imageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            captureSession.setRepeatingRequest(builder.build(), null, backgroundHandler);
            Log.i(TAG, "Camera capture started");
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to start capture", e);
        }
    }

    private void processImage(Image image) {
        try {
            Image.Plane[] planes = image.getPlanes();
            if (planes.length >= 3) {
                Image.Plane yPlane = planes[0];
                Image.Plane uPlane = planes[1];
                Image.Plane vPlane = planes[2];

                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();

                ByteBuffer yBuffer = yPlane.getBuffer();
                int ySize = yBuffer.remaining();
                byte[] yData = new byte[ySize];
                yBuffer.get(yData);

                ByteBuffer uBuffer = uPlane.getBuffer();
                int uSize = uBuffer.remaining();
                byte[] uData = new byte[uSize];
                uBuffer.get(uData);

                ByteBuffer vBuffer = vPlane.getBuffer();
                int vSize = vBuffer.remaining();
                byte[] vData = new byte[vSize];
                vBuffer.get(vData);

                Log.d(TAG, "YUV data sizes - Y: " + ySize + ", U: " + uSize + ", V: " + vSize);

                byte[] rgbaData = convertYuvToRgba(yData, uData, vData, imageWidth, imageHeight,
                    yPlane.getRowStride(), uPlane.getRowStride(), vPlane.getRowStride(),
                    yPlane.getPixelStride(), uPlane.getPixelStride(), vPlane.getPixelStride());

                if (rgbaData != null) {
                    latestFrameData = rgbaData;
                    Log.v(TAG, "Sending RGBA data to native: size=" + rgbaData.length);
                    onFrameAvailable(rgbaData, frameWidth, frameHeight);
                }
            } else {
                Log.w(TAG, "Not enough planes for color processing, falling back to grayscale");
                processImageGrayscale(image);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
        }
    }

    private void processImageGrayscale(Image image) {
        try {
            Image.Plane[] planes = image.getPlanes();
            if (planes.length > 0) {
                Image.Plane yPlane = planes[0];
                ByteBuffer yBuffer = yPlane.getBuffer();

                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();

                byte[] yData = new byte[frameWidth * frameHeight];
                int pixelStride = yPlane.getPixelStride();
                int rowStride = yPlane.getRowStride();

                if (pixelStride == 1 && rowStride == imageWidth) {
                    yBuffer.get(yData);
                } else {
                    byte[] rowData = new byte[rowStride];
                    for (int row = 0; row < Math.min(imageHeight, frameHeight); row++) {
                        if (yBuffer.remaining() >= rowStride) {
                            yBuffer.get(rowData, 0, rowStride);
                            for (int col = 0; col < Math.min(imageWidth, frameWidth); col += pixelStride) {
                                yData[row * frameWidth + col] = rowData[col];
                            }
                        }
                    }
                }

                byte[] rgbaData = convertGrayscaleToRgba(yData, frameWidth, frameHeight);
                if (rgbaData != null) {
                    latestFrameData = rgbaData;
                    onFrameAvailable(rgbaData, frameWidth, frameHeight);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in grayscale processing", e);
        }
    }

    private byte[] convertYuvToRgba(byte[] yData, byte[] uData, byte[] vData, int width, int height,
                                   int yRowStride, int uRowStride, int vRowStride,
                                   int yPixelStride, int uPixelStride, int vPixelStride) {
        byte[] rgba = new byte[frameWidth * frameHeight * 4];

        try {
            int uvHeight = height / 2;
            int uvWidth = width / 2;

            for (int row = 0; row < frameHeight && row < height; row++) {
                for (int col = 0; col < frameWidth && col < width; col++) {
                    int yIndex = row * yRowStride + col * yPixelStride;
                    int yValue = (yIndex < yData.length) ? (yData[yIndex] & 0xFF) : 128;

                    int uvRow = row / 2;
                    int uvCol = col / 2;
                    int uIndex = uvRow * uRowStride + uvCol * uPixelStride;
                    int vIndex = uvRow * vRowStride + uvCol * vPixelStride;

                    int uValue = (uIndex < uData.length) ? (uData[uIndex] & 0xFF) : 128;
                    int vValue = (vIndex < vData.length) ? (vData[vIndex] & 0xFF) : 128;

                    int y = yValue;
                    int u = uValue - 128;
                    int v = vValue - 128;

                    int r = (int)(y + 1.402f * v);
                    int g = (int)(y - 0.344f * u - 0.714f * v);
                    int b = (int)(y + 1.772f * u);

                    r = Math.max(0, Math.min(255, r));
                    g = Math.max(0, Math.min(255, g));
                    b = Math.max(0, Math.min(255, b));

                    int pixelIndex = (row * frameWidth + col) * 4;
                    rgba[pixelIndex] = (byte)b;
                    rgba[pixelIndex + 1] = (byte)g;
                    rgba[pixelIndex + 2] = (byte)r;
                    rgba[pixelIndex + 3] = (byte)255;
                }
            }

            Log.d(TAG, "RGB conversion sample - R:" + (rgba[2] & 0xFF) + " G:" + (rgba[1] & 0xFF) + " B:" + (rgba[0] & 0xFF));
            return rgba;
        } catch (Exception e) {
            Log.e(TAG, "Error in YUV to RGBA conversion", e);
            return convertGrayscaleToRgba(yData, frameWidth, frameHeight);
        }
    }

    private byte[] convertGrayscaleToRgba(byte[] yData, int width, int height) {
        byte[] rgba = new byte[width * height * 4];
        for (int i = 0; i < width * height; i++) {
            int y = yData[i] & 0xFF;
            rgba[i * 4] = (byte)y;
            rgba[i * 4 + 1] = (byte)y;
            rgba[i * 4 + 2] = (byte)y;
            rgba[i * 4 + 3] = (byte)255;
        }
        return rgba;
    }

    public void closeCamera() {
        isCapturing = false;
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        stopBackgroundThread();
        Log.i(TAG, "Camera closed");
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }

    public byte[] getLatestFrame() {
        return latestFrameData;
    }
}