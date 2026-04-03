package com.example.takeit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private CameraManager cameraManager;
    private ImageView ivShutter;
    private ImageView ivFormat;
    private ImageView ivFlash;
    private boolean isCapturing = false;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), results -> {
                Boolean cameraGranted = results.get(Manifest.permission.CAMERA);
                if (Boolean.TRUE.equals(cameraGranted)) {
                    cameraManager.startCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        PreviewView previewView = findViewById(R.id.previewView);
        ivFormat = findViewById(R.id.btnFormat);
        ivFlash = findViewById(R.id.ivFlash);
        ivShutter = findViewById(R.id.ivShutter);

        cameraManager = new CameraManager(this, previewView);
        ivFlash.setVisibility(View.GONE);
        ivFormat.setVisibility(View.GONE);
        cameraManager.setOnRawSupportListener(supported ->
                ivFormat.setVisibility(supported ? View.VISIBLE : View.GONE));
        ivFormat.setOnClickListener(v -> toggleFormat());

        cameraManager.setOnCameraReadyListener(hasFlashUnit -> {
            if (hasFlashUnit && cameraManager.getCurrentFormat() == CameraManager.CaptureFormat.JPEG) {
                ivFlash.setVisibility(View.VISIBLE);
            } else {
                ivFlash.setVisibility(View.GONE);
            }

            ivFlash.setOnClickListener(v -> cycleLightMode());
            updateFlashIcon(cameraManager.getLightMode());
        });

        ivShutter.setOnClickListener(v -> {
            if (isCapturing) return;
            isCapturing = true;
            ivShutter.setImageResource(R.drawable.ic_shutter_pressed);
            ivFormat.setEnabled(false);

            cameraManager.capturePhoto(new CameraManager.OnPhotoCapturedListener() {
                @Override
                public void onSuccess() {
                    isCapturing = false;
                    ivShutter.setImageResource(R.drawable.ic_shutter_idle);
                    ivFormat.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Photo saved!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    isCapturing = false;
                    ivShutter.setImageResource(R.drawable.ic_shutter_idle);
                    ivFormat.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Failed: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            cameraManager.startCamera();
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            });
        }
    }

    private void toggleFormat() {
        if (cameraManager.getCurrentFormat() == CameraManager.CaptureFormat.JPEG) {
            cameraManager.setFormat(CameraManager.CaptureFormat.RAW);
            ivFormat.setImageResource(R.drawable.ic_button_format_raw);
        } else {
            cameraManager.setFormat(CameraManager.CaptureFormat.JPEG);
            ivFormat.setImageResource(R.drawable.ic_button_format_jpeg);
        }
    }

    private void cycleLightMode() {
        int next;
        switch (cameraManager.getLightMode()) {
            case CameraManager.LIGHT_MODE_OFF:
                next = CameraManager.LIGHT_MODE_AUTO;
                break;
            case CameraManager.LIGHT_MODE_AUTO:
                next = CameraManager.LIGHT_MODE_TORCH;
                break;
            default:
                next = CameraManager.LIGHT_MODE_OFF;
                break;
        }
        cameraManager.setLightMode(next);
        updateFlashIcon(next);
    }

    private void updateFlashIcon(int mode) {
        switch (mode) {
            case CameraManager.LIGHT_MODE_TORCH:
                ivFlash.setImageResource(R.drawable.ic_flash_on);
                break;
            case CameraManager.LIGHT_MODE_AUTO:
                ivFlash.setImageResource(R.drawable.ic_flash_automatic);
                break;
            default:
                ivFlash.setImageResource(R.drawable.ic_flash_off);
                break;
        }
    }
}