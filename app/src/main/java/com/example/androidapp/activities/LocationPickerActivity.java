package com.example.androidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.androidapp.R;

import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

public class LocationPickerActivity extends AppCompatActivity implements MapEventsReceiver {

    private MapView mapView;
    private IMapController mapController;
    private Marker selectedMarker;
    private double latitude = 0;
    private double longitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        // Configurar el mapa de OSMDroid
        mapView = findViewById(R.id.mapview);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Configurar el controlador del mapa
        mapController = mapView.getController();
        mapController.setZoom(15.0);

        // Centrar en una ubicación por defecto (ej. Madrid)
        GeoPoint startPoint = new GeoPoint(40.416775, -3.703790);
        mapController.setCenter(startPoint);

        // Agregar listener para clics en el mapa
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(this, this);
        mapView.getOverlays().add(eventsOverlay);

        // Agregar botón para confirmar selección
        findViewById(R.id.btnConfirmLocation).setOnClickListener(v -> {
            if (selectedMarker != null) {
                Intent result = new Intent();
                result.putExtra("latitude", latitude);
                result.putExtra("longitude", longitude);
                setResult(RESULT_OK, result);
                finish();
            } else {
                Toast.makeText(this, "Seleccione una ubicación en el mapa", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        // Limpiar marcador anterior si existe
        if (selectedMarker != null) {
            mapView.getOverlays().remove(selectedMarker);
        }

        // Crear nuevo marcador de OSMDroid
        selectedMarker = new Marker(mapView);
        selectedMarker.setPosition(p);
        selectedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        selectedMarker.setTitle("Ubicación seleccionada");

        // Agregar marcador al mapa
        mapView.getOverlays().add(selectedMarker);
        mapView.invalidate();

        // Guardar coordenadas
        latitude = p.getLatitude();
        longitude = p.getLongitude();

        return true;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        // No necesitamos implementar esto para selección simple
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}