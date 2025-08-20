package com.example.androidapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.androidapp.R;
import com.example.androidapp.database.DatabaseHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;

public class ViewReportsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ListView reportsList;
    private TextView tvEmpty;

    private Cursor cursor; // 🔹 Ahora mantenemos el cursor como campo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_reports);

        dbHelper = new DatabaseHelper(this);
        reportsList = findViewById(R.id.reportsList);
        tvEmpty = findViewById(R.id.tvEmpty);

        FloatingActionButton fabAddReport = findViewById(R.id.fabAddReport);
        fabAddReport.setOnClickListener(v -> {
            startActivity(new Intent(ViewReportsActivity.this, AddReportActivity.class));
        });

        loadReports();
    }

    private void loadReports() {
        int userId = getUserId();
        Log.d("ViewReports", "Usuario ID: " + userId);

        // 🔹 Cerrar cursor anterior si existe
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        cursor = dbHelper.getUserReports(userId);

        if (cursor == null) {
            Log.e("ViewReports", "Cursor es null");
            tvEmpty.setText("Error al cargar reportes");
            tvEmpty.setVisibility(View.VISIBLE);
            reportsList.setVisibility(View.GONE);
            return;
        }

        Log.d("ViewReports", "Número de reportes: " + cursor.getCount());

        if (cursor.getCount() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            reportsList.setVisibility(View.GONE);
            return;
        }

        // 🔹 Verificar columnas del cursor
        String[] columns = cursor.getColumnNames();
        Log.d("ViewReports", "Columnas: " + Arrays.toString(columns));

        String[] fromColumns = { DatabaseHelper.COLUMN_PLACE, DatabaseHelper.COLUMN_RATING };
        int[] toViews = { R.id.tvReportPlace, R.id.tvReportRating };

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this, R.layout.report_list_item, cursor, fromColumns, toViews, 0
        );
        reportsList.setAdapter(adapter);
    }

    private String getRatingStars(int rating) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < rating; i++) {
            stars.append("★");
        }
        for (int i = rating; i < 5; i++) {
            stars.append("☆");
        }
        return stars.toString();
    }

    private int getUserId() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        return prefs.getInt("user_id", -1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReports(); // 🔄 Recargar reportes al volver
    }

    @Override
    protected void onDestroy() {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close(); // ✅ Cerrar cursor al destruir la actividad
        }
        dbHelper.close();
        super.onDestroy();
    }
}
