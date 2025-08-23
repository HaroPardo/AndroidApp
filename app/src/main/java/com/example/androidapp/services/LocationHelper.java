package com.example.androidapp.services;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
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

    public static void startLocationMonitoring(Context context, long reminderId, double latitude,
                                               double longitude, float radius, String title, int repeatType) {
        // Si es de una sola vez -> geofence tradicional
        if (repeatType == 0) {
            setupGeofence(context, reminderId, latitude, longitude, radius, title);
        }
        // Si es repetitivo -> usar servicio de monitoreo
        else {
            startMonitoringService(context, reminderId);
        }
    }

    private static void setupGeofence(Context context, long reminderId, double latitude,
                                      double longitude, float radius, String title) {
        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);
        String requestId = "geo_" + reminderId;

        Geofence geofence = new Geofence.Builder()
                .setRequestId(requestId)
                .setCircularRegion(latitude, longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(5 * 60 * 1000) // 5 minutos
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .addGeofence(geofence)
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .build();

        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("reminder_id", String.valueOf(reminderId));
        intent.putExtra("repeat_type", 0);

        PendingIntent geofencePendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
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

    private static void startMonitoringService(Context context, long reminderId) {
        Intent serviceIntent = new Intent(context, LocationMonitoringService.class);

        // Iniciar el servicio si no está en ejecución
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // Agregar el recordatorio al monitoreo
        try {
            // Esperar a que el servicio arranque
            Thread.sleep(1000);

            Intent bindIntent = new Intent(context, LocationMonitoringService.class);
            context.bindService(bindIntent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    LocationMonitoringService.LocalBinder binder =
                            (LocationMonitoringService.LocalBinder) service;
                    LocationMonitoringService monitoringService = binder.getService();
                    monitoringService.addReminderToMonitor(reminderId);
                    context.unbindService(this);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {}
            }, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e(TAG, "Error al agregar recordatorio al servicio", e);
        }
    }

    public static void removeGeofence(Context context, long reminderId) {
        // Primero, intentar quitar del servicio de monitoreo
        try {
            Intent bindIntent = new Intent(context, LocationMonitoringService.class);
            context.bindService(bindIntent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    LocationMonitoringService.LocalBinder binder =
                            (LocationMonitoringService.LocalBinder) service;
                    LocationMonitoringService monitoringService = binder.getService();
                    monitoringService.removeReminderFromMonitor(reminderId);
                    context.unbindService(this);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {}
            }, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e(TAG, "Error al eliminar recordatorio del servicio", e);
        }

        // Luego, eliminar geofence tradicional si existe
        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);
        List<String> geofenceIds = new ArrayList<>();
        geofenceIds.add("geo_" + reminderId);

        geofencingClient.removeGeofences(geofenceIds)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Geofence eliminado: geo_" + reminderId))
                .addOnFailureListener(e -> Log.e(TAG, "Error al eliminar geofence", e));
    }

    public static void removeAllGeofences(Context context) {
        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);
        geofencingClient.removeGeofences(getGeofencePendingIntent(context))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Todos los geofences eliminados"))
                .addOnFailureListener(e -> Log.e(TAG, "Error al eliminar todos los geofences", e));
    }

    private static PendingIntent getGeofencePendingIntent(Context context) {
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}


