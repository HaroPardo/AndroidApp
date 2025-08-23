package com.example.androidapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.androidapp.R;
import com.example.androidapp.activities.ReminderDetailActivity;
import com.example.androidapp.database.DatabaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.HashSet;
import java.util.Set;

public class LocationMonitoringService extends Service {
    private static final String TAG = "LocationMonitoringService";
    private static final String CHANNEL_ID = "location_monitoring_channel";
    private static final int NOTIFICATION_ID = 100;
    private static final long CHECK_INTERVAL = 5 * 60 * 1000; // 5 minutos

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Handler handler;
    private Runnable locationCheckRunnable;
    private Set<Long> activeReminders = new HashSet<>();
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        LocationMonitoringService getService() {
            return LocationMonitoringService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        loadActiveReminders();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        startLocationUpdates();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        handler.removeCallbacks(locationCheckRunnable);
    }

    public void addReminderToMonitor(long reminderId) {
        activeReminders.add(reminderId);
        saveActiveReminders();
    }

    public void removeReminderFromMonitor(long reminderId) {
        activeReminders.remove(reminderId);
        saveActiveReminders();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Monitoreo de Ubicación",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, ReminderDetailActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Monitoreo de recordatorios")
                .setContentText("Monitoreando ubicaciones para recordatorios repetitivos")
                .setSmallIcon(R.drawable.ic_location_reminder)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, CHECK_INTERVAL)
                .setMinUpdateIntervalMillis(CHECK_INTERVAL / 2)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    checkAllReminders(location);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } catch (SecurityException e) {
            Log.e(TAG, "Permisos de ubicación no concedidos", e);
        }
    }

    private void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void checkAllReminders(Location currentLocation) {
        if (activeReminders.isEmpty()) return;

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        for (Long reminderId : activeReminders) {
            Cursor cursor = dbHelper.getReminderById(String.valueOf(reminderId));
            if (cursor != null && cursor.moveToFirst()) {
                double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LATITUDE));
                double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LONGITUDE));
                float radius = cursor.getFloat(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_RADIUS));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TITLE));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESCRIPTION));
                int repeatType = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPEAT_TYPE));

                // Solo procesar recordatorios con repetición
                if (repeatType == 1) {
                    float[] results = new float[1];
                    Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                            latitude, longitude, results);

                    if (results[0] <= radius) {
                        showNotification(title, description, (int) (long) reminderId);
                    }
                }
                cursor.close();
            }
        }
        dbHelper.close();
    }

    private void showNotification(String title, String description, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Crear canal de notificación si es necesario
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios de Ubicación",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones cuando estás en ubicaciones específicas");
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_location_reminder)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(notificationId, builder.build());
    }

    private void loadActiveReminders() {
        SharedPreferences prefs = getSharedPreferences("active_reminders", MODE_PRIVATE);
        activeReminders = prefs.getStringSet("reminder_ids", new HashSet<>())
                .stream()
                .map(Long::parseLong)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }

    private void saveActiveReminders() {
        SharedPreferences prefs = getSharedPreferences("active_reminders", MODE_PRIVATE);
        Set<String> reminderIds = new HashSet<>();
        for (Long id : activeReminders) {
            reminderIds.add(String.valueOf(id));
        }
        prefs.edit().putStringSet("reminder_ids", reminderIds).apply();
    }
}
