package com.example.androidapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.androidapp.R;
import com.example.androidapp.database.DatabaseHelper;

public class RemindersActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ListView remindersList;
    private int userId;
    private SimpleCursorAdapter adapter;
    private TextView tvEmpty;
    private Cursor cursor; // Añadir referencia al cursor

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        dbHelper = new DatabaseHelper(this);
        userId = getUserId();

        Button btnAddReminder = findViewById(R.id.btnAddReminder);
        remindersList = findViewById(R.id.remindersList);
        tvEmpty = findViewById(R.id.tvEmpty);

        btnAddReminder.setOnClickListener(v -> {
            startActivity(new Intent(RemindersActivity.this, AddReminderActivity.class));
        });

        loadReminders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReminders(); // Recargar la lista cuando se vuelva a esta actividad
    }

    private int getUserId() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        return prefs.getInt("user_id", -1);
    }

    private void loadReminders() {
        // Cerrar cursor anterior si existe
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        cursor = dbHelper.getUserReminders(userId);

        if (cursor.getCount() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            remindersList.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            remindersList.setVisibility(View.VISIBLE);

            String[] fromColumns = {DatabaseHelper.COLUMN_TITLE, DatabaseHelper.COLUMN_CREATED_AT};
            int[] toViews = {R.id.tvReminderTitle, R.id.tvReminderDate};

            adapter = new SimpleCursorAdapter(
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
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cerrar el cursor cuando la actividad se destruye
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        dbHelper.close();
    }
}
