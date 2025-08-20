package com.example.androidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.androidapp.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class LocationPickerActivity extends AppCompatActivity implements
        OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    private Marker selectedMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);

        // Centrar en una ubicación por defecto (ej. Madrid)
        LatLng madrid = new LatLng(40.416775, -3.703790);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(madrid, 12));

        // Agregar botón para confirmar selección
        findViewById(R.id.btnConfirmLocation).setOnClickListener(v -> {
            if (selectedMarker != null) {
                Intent result = new Intent();
                result.putExtra("latitude", selectedMarker.getPosition().latitude);
                result.putExtra("longitude", selectedMarker.getPosition().longitude);
                setResult(RESULT_OK, result);
                finish();
            } else {
                Toast.makeText(this, "Seleccione una ubicación en el mapa", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (selectedMarker != null) {
            selectedMarker.remove();
        }
        selectedMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Ubicación seleccionada"));
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }
}
