package com.example.takeit;

import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureCapabilities;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CameraManager {
    public enum CaptureFormat {JPEG, RAW}

    private final LifecycleOwner lifecycleOwner;
    private final PreviewView previewView;
    private final Context context;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ImageCapture imageCapture;
    private CaptureFormat currentFormat = CaptureFormat.JPEG;
    private final int currentCameraFacing = CameraSelector.LENS_FACING_BACK;

    private OnRawSupportListener rawSupportListener;

    public interface OnRawSupportListener {
        void onRawSupported(boolean supported);
    }

    public interface OnPhotoCapturedListener {
        void onSuccess();

        void onError(String message);
    }

    public CameraManager(LifecycleOwner lifecycleOwner, PreviewView previewView) {
        this.lifecycleOwner = lifecycleOwner;
        this.previewView = previewView;
        this.context = previewView.getContext();
    }

    public void setOnRawSupportListener(OnRawSupportListener listener) {
        this.rawSupportListener = listener;
    }

    private boolean checkRawSupport(CameraSelector selector) {
        CameraInfo cameraInfo = cameraProvider.getCameraInfo(selector);
        ImageCaptureCapabilities capabilities = ImageCapture.getImageCaptureCapabilities(cameraInfo);
        return capabilities.getSupportedOutputFormats().contains(ImageCapture.OUTPUT_FORMAT_RAW);
    }

    private ImageCapture buildImageCapture(boolean rawSupported) {
        ImageCapture.Builder builder = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY);

        if (currentFormat == CaptureFormat.RAW && rawSupported) {
            builder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_RAW);
        }

        return builder.build();
    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(currentCameraFacing)
                        .build();

                boolean rawSupported = checkRawSupport(cameraSelector);

                if (!rawSupported && currentFormat == CaptureFormat.RAW) {
                    currentFormat = CaptureFormat.JPEG;
                }

                if (rawSupportListener != null) {
                    rawSupportListener.onRawSupported(rawSupported);
                }

                imageCapture = buildImageCapture(rawSupported);

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraManager", "Failed to start camera", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public void capturePhoto(OnPhotoCapturedListener listener) {
        if (imageCapture == null) {
            if (listener != null) listener.onError("Camera not ready");
            return;
        }

        String name = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis());

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE,
                currentFormat == CaptureFormat.RAW ? "image/x-adobe-dng" : "image/jpeg");

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TakeIt");
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                context.getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        ).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.@NonNull OutputFileResults outputFileResults) {
                        if (listener != null) listener.onSuccess();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("CameraManager", "Capture failed", exception);
                        if (listener != null) listener.onError(exception.getMessage());
                    }
                }
        );
    }

    public void setFormat(CaptureFormat format) {
        if (currentFormat == format) return;
        currentFormat = format;
        startCamera();
    }

    public CaptureFormat getCurrentFormat() {
        return currentFormat;
    }
}
