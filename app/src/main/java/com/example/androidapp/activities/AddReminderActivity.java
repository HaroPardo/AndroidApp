package com.example.androidapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.androidapp.R;
import com.example.androidapp.database.DatabaseHelper;
import com.example.androidapp.services.LocationHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class AddReminderActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private static final int REQUEST_IMAGE_CAPTURE = 1002;
    private static final int REQUEST_MAP_SELECTION = 1003;

    private EditText etTitle, etDescription;
    private RadioGroup rgLocationType;
    private Button btnAddImage, btnSaveReminder;
    private ImageView ivImagePreview;

    private double latitude = 0;
    private double longitude = 0;
    private float radius = 100; // metros
    private String imagePath = null;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        userId = getUserId();

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        rgLocationType = findViewById(R.id.rgLocationType);
        btnAddImage = findViewById(R.id.btnAddImage);
        btnSaveReminder = findViewById(R.id.btnSaveReminder);
        ivImagePreview = findViewById(R.id.ivImagePreview);

        btnAddImage.setOnClickListener(v -> {
            // Implementar captura de imagen similar a ReportActivity
            Toast.makeText(this, "Funcionalidad de imagen no implementada", Toast.LENGTH_SHORT).show();
        });

        btnSaveReminder.setOnClickListener(v -> saveReminder());
    }

    private void saveReminder() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Ingrese un título", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = rgLocationType.getCheckedRadioButtonId();

        if (selectedId == R.id.rbCurrentLocation) {
            requestLocation();
        } else if (selectedId == R.id.rbMapLocation) {
            startActivityForResult(new Intent(this, LocationPickerActivity.class), REQUEST_MAP_SELECTION);
        } else {
            Toast.makeText(this, "Seleccione un tipo de ubicación", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationServices.getFusedLocationProviderClient(this)
                .getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        saveReminderToDatabase();
                    } else {
                        Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveReminderToDatabase() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        long reminderId = dbHelper.addReminder(userId, title, latitude, longitude, radius, imagePath);
        dbHelper.close();

        if (reminderId != -1) {
            Toast.makeText(this, "Recordatorio creado", Toast.LENGTH_SHORT).show();

            // Iniciar monitoreo de ubicación
            LocationHelper.startLocationMonitoring(this, latitude, longitude, radius, title);

            finish();
        } else {
            Toast.makeText(this, "Error al guardar recordatorio", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_MAP_SELECTION && resultCode == RESULT_OK) {
            latitude = data.getDoubleExtra("latitude", 0);
            longitude = data.getDoubleExtra("longitude", 0);
            saveReminderToDatabase();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int getUserId() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        return prefs.getInt("user_id", -1);
    }
}
