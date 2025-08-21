package com.example.androidapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.androidapp.R;
import com.example.androidapp.database.DatabaseHelper;

public class RemindersActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ListView remindersList;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        dbHelper = new DatabaseHelper(this);
        userId = getUserId();

        Button btnAddReminder = findViewById(R.id.btnAddReminder);
        remindersList = findViewById(R.id.remindersList);

        btnAddReminder.setOnClickListener(v -> {
            startActivity(new Intent(RemindersActivity.this, AddReminderActivity.class));
        });

        loadReminders();
    }

    private void loadReminders() {
        Cursor cursor = null;
        try {
            cursor = dbHelper.getUserReminders(userId);

            if (cursor == null || cursor.getCount() == 0) {
                // Mostrar mensaje de lista vacía
                findViewById(R.id.tvEmpty).setVisibility(View.VISIBLE);
                remindersList.setAdapter(null);
                return;
            } else {
                findViewById(R.id.tvEmpty).setVisibility(View.GONE);
            }

            String[] fromColumns = {
                    DatabaseHelper.COLUMN_TITLE,
                    DatabaseHelper.COLUMN_CREATED_AT
            };

            int[] toViews = {
                    R.id.tvReminderTitle,
                    R.id.tvReminderDate
            };

            SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                    this,
                    R.layout.reminder_list_item,
                    cursor,
                    fromColumns,
                    toViews,
                    0
            );

            remindersList.setAdapter(adapter);

            remindersList.setOnItemClickListener((parent, view, position, id) -> {
                Intent intent = new Intent(RemindersActivity.this, ReminderDetailActivity.class);
                intent.putExtra("reminder_id", id);
                startActivity(intent);
            });

        } catch (Exception e) {
            Log.e("RemindersActivity", "Error loading reminders", e);
            Toast.makeText(this, "Error al cargar recordatorios", Toast.LENGTH_SHORT).show();
        } finally {
            // No cerramos el cursor aquí porque el adapter lo necesita
            // El adapter se encargará de manejarlo.
        }
    }


    private int getUserId() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        return prefs.getInt("user_id", -1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReminders();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
