package com.example.androidapp.services;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Collections;

public class LocationHelper {

    private static final String TAG = "LocationHelper";

    public static void startLocationMonitoring(Context context, double latitude, double longitude, float radius, String title) {
        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);

        Geofence geofence = new Geofence.Builder()
                .setRequestId("geo_" + System.currentTimeMillis())
                .setCircularRegion(latitude, longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .addGeofence(geofence)
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .build();

        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        intent.putExtra("title", title);
        PendingIntent geofencePendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Geofence añadido: " + title))
                    .addOnFailureListener(e -> Log.e(TAG, "Error al añadir geofence", e));
        } catch (SecurityException e) {
            Log.e(TAG, "Error de permisos al añadir geofence", e);
        }
    }
}
