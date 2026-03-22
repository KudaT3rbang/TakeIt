package com.example.takeit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
        ivShutter   = findViewById(R.id.ivShutter);

        cameraManager = new CameraManager(this, previewView);

        ivShutter.setOnClickListener(v -> {
            if (isCapturing) return;
            isCapturing = true;
            ivShutter.setImageResource(R.drawable.ic_shutter_pressed);

            cameraManager.capturePhoto(new CameraManager.OnPhotoCapturedListener() {
                @Override
                public void onSuccess() {
                    isCapturing = false;
                    ivShutter.setImageResource(R.drawable.ic_shutter_idle);
                    Toast.makeText(MainActivity.this, "Photo saved!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    isCapturing = false;
                    ivShutter.setImageResource(R.drawable.ic_shutter_idle);
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
}