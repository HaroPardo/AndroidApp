package com.example.androidapp.services;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

public class LocationHelper {

    private static final String TAG = "LocationHelper";

    public static void startLocationMonitoring(Context context, long reminderId, double latitude, double longitude, float radius, String title) {
        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);

        // Usar el ID del recordatorio como requestId para poder eliminarlo después
        String requestId = "geo_" + reminderId;

        Geofence geofence = new Geofence.Builder()
                .setRequestId(requestId)
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
        intent.putExtra("reminder_id", String.valueOf(reminderId));
        PendingIntent geofencePendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        try {
            // Verificar permisos antes de añadir geofences
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "No tiene permisos de ubicación");
                return;
            }

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Geofence añadido: " + requestId))
                    .addOnFailureListener(e -> Log.e(TAG, "Error al añadir geofence", e));
        } catch (SecurityException e) {
            Log.e(TAG, "Error de permisos al añadir geofence", e);
        }
    }

    // Método para eliminar un geofence específico
    public static void removeGeofence(Context context, long reminderId) {
        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);

        // Crear una lista con el ID del geofence a eliminar
        List<String> geofenceIds = new ArrayList<>();
        geofenceIds.add("geo_" + reminderId);

        geofencingClient.removeGeofences(geofenceIds)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Geofence eliminado: geo_" + reminderId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error al eliminar geofence", e));
    }

    // Método para eliminar todos los geofences
    public static void removeAllGeofences(Context context) {
        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);

        geofencingClient.removeGeofences(getGeofencePendingIntent(context))
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Todos los geofences eliminados"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error al eliminar todos los geofences", e));
    }

    // Método auxiliar para obtener el PendingIntent
    private static PendingIntent getGeofencePendingIntent(Context context) {
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}

