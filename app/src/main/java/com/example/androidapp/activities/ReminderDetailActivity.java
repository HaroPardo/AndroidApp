package com.example.androidapp.activities;

import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.androidapp.services.LocationHelper;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.androidapp.R;
import com.example.androidapp.database.DatabaseHelper;
import com.squareup.picasso.Picasso;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ReminderDetailActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_detail);

        // Configurar OSMDroid
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));

        dbHelper = new DatabaseHelper(this);
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        long reminderId = getIntent().getLongExtra("reminder_id", -1);
        if (reminderId == -1) {
            Toast.makeText(this, "Error al cargar el recordatorio", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Configurar botón de eliminar
        Button btnDelete = findViewById(R.id.btnDeleteReminder);
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog(reminderId));

        loadReminderDetails(reminderId);
    }

    private void loadReminderDetails(long reminderId) {
        Cursor cursor = dbHelper.getReminderById(String.valueOf(reminderId));
        if (cursor != null && cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TITLE));
            String description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESCRIPTION));
            double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LATITUDE));
            double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LONGITUDE));
            String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IMAGE_PATH));

            TextView tvTitle = findViewById(R.id.tvTitle);
            TextView tvDescription = findViewById(R.id.tvDescription);
            TextView tvLocation = findViewById(R.id.tvLocation);
            ImageView ivReminderImage = findViewById(R.id.ivReminderImage);

            tvTitle.setText(title);
            tvDescription.setText(description.isEmpty() ? "Sin descripción" : description);

            // Obtener dirección a partir de las coordenadas
            String address = getAddressFromLocation(latitude, longitude);
            tvLocation.setText(address.isEmpty() ?
                    String.format(Locale.getDefault(), "Coordenadas: %.6f, %.6f", latitude, longitude) :
                    address);

            // Configurar el mapa
            setupMap(latitude, longitude, title);

            // Cargar imagen si existe
            if (imagePath != null && !imagePath.isEmpty()) {
                try {
                    Picasso.get().load("file://" + imagePath).into(ivReminderImage);
                    findViewById(R.id.imageSection).setVisibility(android.view.View.VISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            cursor.close();
        }
        dbHelper.close();
    }

    private String getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    sb.append(address.getAddressLine(i));
                    if (i < address.getMaxAddressLineIndex()) {
                        sb.append(", ");
                    }
                }
                return sb.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void setupMap(double latitude, double longitude, String title) {
        try {
            GeoPoint point = new GeoPoint(latitude, longitude);
            org.osmdroid.views.MapController mapController = (org.osmdroid.views.MapController) mapView.getController();
            mapController.setZoom(15.0);
            mapController.setCenter(point);

            Marker marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(title);
            mapView.getOverlays().add(marker);
        } catch (Exception e) {
            e.printStackTrace();
            mapView.setVisibility(android.view.View.GONE);
            Toast.makeText(this, "Error al cargar el mapa", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmationDialog(long reminderId) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Recordatorio")
                .setMessage("¿Estás seguro de que quieres eliminar este recordatorio?")
                .setPositiveButton("Eliminar", (dialog, which) -> deleteReminder(reminderId))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteReminder(long reminderId) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);

        // Eliminar geofence asociado
        LocationHelper.removeGeofence(this, reminderId);

        boolean success = dbHelper.deleteReminder(reminderId);
        dbHelper.close();

        if (success) {
            Toast.makeText(this, "Recordatorio eliminado correctamente", Toast.LENGTH_SHORT).show();
            finish(); // Volver a la actividad anterior
        } else {
            Toast.makeText(this, "Error al eliminar el recordatorio", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}


