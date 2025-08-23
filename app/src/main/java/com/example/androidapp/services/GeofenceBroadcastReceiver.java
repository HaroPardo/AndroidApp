package com.example.androidapp.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.androidapp.R;
import com.example.androidapp.database.DatabaseHelper;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "location_reminder_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Manejar tanto ENTER como DWELL events
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {

            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            for (Geofence geofence : triggeringGeofences) {
                String title = intent.getStringExtra("title");
                String reminderId = intent.getStringExtra("reminder_id");
                int repeatType = intent.getIntExtra("repeat_type", 0);

                if (title == null) {
                    title = "Recordatorio de ubicación";
                }

                // Consultar la base de datos para obtener más detalles
                DatabaseHelper dbHelper = new DatabaseHelper(context);
                Cursor cursor = dbHelper.getReminderById(reminderId);

                if (cursor != null && cursor.moveToFirst()) {
                    String description = cursor.getString(
                            cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESCRIPTION)
                    );

                    // Mostrar notificación con ID único para cada recordatorio
                    showNotification(context, title, description, Integer.parseInt(reminderId));

                    // Si es de una sola vez, eliminar el recordatorio después de mostrar la notificación
                    if (repeatType == 0) {
                        LocationHelper.removeGeofence(context, Long.parseLong(reminderId));
                        dbHelper.deleteReminder(Long.parseLong(reminderId));
                    }

                    cursor.close();
                } else {
                    // Si no se encuentra en BD, mostrar notificación básica
                    showNotification(context, title, "Has llegado a la ubicación programada", Integer.parseInt(reminderId));
                }

                dbHelper.close();
            }
        }
    }

    private void showNotification(Context context, String title, String description, int notificationId) {
        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_location_reminder)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Estilo expandible con imagen
        NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle()
                .bigPicture(BitmapFactory.decodeResource(context.getResources(), R.drawable.map_placeholder))
                .setBigContentTitle(title)
                .setSummaryText(description);
        builder.setStyle(bigPictureStyle);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Recordatorios de Ubicación";
            String description = "Notificaciones cuando llegas a ubicaciones específicas";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 250, 500});

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
