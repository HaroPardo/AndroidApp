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
    private Cursor cursor; // Mantener referencia al cursor

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

        // Configurar el listener para clics en elementos de la lista
        reportsList.setOnItemClickListener((parent, view, position, id) -> {
            // Abrir actividad de detalles con el ID del reporte
            Intent intent = new Intent(ViewReportsActivity.this, ReportDetailActivity.class);
            intent.putExtra("REPORT_ID", id);
            startActivity(intent);
        });
    }

    private void loadReports() {
        int userId = getUserId();
        Log.d("ViewReports", "Cargando reportes para usuario: " + userId);

        // Cerrar cursor anterior si existe
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        cursor = dbHelper.getUserReports(userId);

        if (cursor == null || cursor.getCount() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            reportsList.setVisibility(View.GONE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        reportsList.setVisibility(View.VISIBLE);

        String[] fromColumns = {
                DatabaseHelper.COLUMN_PLACE,
                DatabaseHelper.COLUMN_RATING
        };
        int[] toViews = {
                R.id.tvReportPlace,
                R.id.tvReportRating
        };

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.report_list_item,
                cursor,
                fromColumns,
                toViews,
                0
        ) {
            // Personalizar cómo se muestran los datos
            @Override
            public void setViewText(TextView v, String text) {
                if (v.getId() == R.id.tvReportRating) {
                    // Convertir el número de rating a estrellas
                    int rating = Integer.parseInt(text);
                    v.setText(getRatingStars(rating));
                } else {
                    super.setViewText(v, text);
                }
            }
        };

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
    protected void onDestroy() {
        // Cerrar el cursor cuando la actividad se destruye
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        dbHelper.close();
        super.onDestroy();
    }
}