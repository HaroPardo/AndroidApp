package com.example.androidapp.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.androidapp.R;
import com.example.androidapp.database.DatabaseHelper;
import com.squareup.picasso.Picasso;

import java.io.File;

public class ReportDetailActivity extends AppCompatActivity {

    private TextView tvPlace, tvExplanation;
    private RatingBar ratingBar;
    private LinearLayout imageContainer;
    private DatabaseHelper dbHelper;
    private long reportId; // guardamos el ID globalmente

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        // Inicializar vistas
        tvPlace = findViewById(R.id.tvPlace);
        ratingBar = findViewById(R.id.ratingBar);
        tvExplanation = findViewById(R.id.tvExplanation);
        imageContainer = findViewById(R.id.imageContainer);
        dbHelper = new DatabaseHelper(this);

        // Obtener el ID del reporte del Intent
        reportId = getIntent().getLongExtra("REPORT_ID", -1);

        if (reportId == -1) {
            Toast.makeText(this, "Error: ID de reporte no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Configurar botón de eliminar
        Button btnDelete = findViewById(R.id.btnDeleteReport);
        btnDelete.setOnClickListener(v -> {
            showDeleteConfirmationDialog(reportId);
        });

        // Cargar detalles del reporte
        loadReportDetails(reportId);
    }

    private void loadReportDetails(long reportId) {
        Cursor cursor = null;
        try {
            cursor = dbHelper.getReportDetails(reportId);

            if (cursor != null && cursor.moveToFirst()) {
                // Obtener datos del cursor
                String place = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PLACE));
                int rating = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_RATING));
                String explanation = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EXPLANATION));
                String images = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IMAGES));

                // Mostrar datos en la interfaz
                tvPlace.setText(place);
                ratingBar.setRating(rating);
                tvExplanation.setText(explanation);

                // Cargar imágenes si existen
                if (images != null && !images.isEmpty()) {
                    loadImages(images);
                } else {
                    // Ocultar sección de imágenes si no hay
                    findViewById(R.id.imageSection).setVisibility(View.GONE);
                }
            } else {
                Toast.makeText(this, "No se encontraron detalles del reporte", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar detalles", Toast.LENGTH_SHORT).show();
            finish();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void loadImages(String imagesString) {
        // Limpiar contenedor de imágenes
        imageContainer.removeAllViews();

        // Dividir la cadena de imágenes (separadas por comas)
        String[] imagePaths = imagesString.split(",");

        for (String path : imagePaths) {
            if (path.trim().isEmpty()) continue;

            try {
                File imgFile = new File(path.trim());
                if (imgFile.exists()) {
                    ImageView imageView = new ImageView(this);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            400, 400
                    );
                    params.setMargins(0, 0, 16, 0);
                    imageView.setLayoutParams(params);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    // Usar Picasso para cargar la imagen con mejor calidad
                    Picasso.get().load(imgFile)
                            .resize(400, 400)
                            .centerCrop()
                            .into(imageView);

                    // Agregar funcionalidad para ampliar imagen al hacer clic
                    imageView.setOnClickListener(v -> {
                        showFullScreenImage(imgFile.getAbsolutePath());
                    });

                    imageContainer.addView(imageView);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Si no se cargaron imágenes, ocultar la sección
        if (imageContainer.getChildCount() == 0) {
            findViewById(R.id.imageSection).setVisibility(View.GONE);
        }
    }

    private void showFullScreenImage(String imagePath) {
        // Crear un diálogo para mostrar la imagen en pantalla completa
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView fullScreenImageView = dialog.findViewById(R.id.fullScreenImageView);
        Button btnClose = dialog.findViewById(R.id.btnClose);

        // Usar Picasso para cargar la imagen en alta calidad
        Picasso.get().load(new File(imagePath)).into(fullScreenImageView);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDeleteConfirmationDialog(long reportId) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Reporte")
                .setMessage("¿Estás seguro de que quieres eliminar este reporte?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    deleteReport(reportId);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteReport(long reportId) {
        boolean success = dbHelper.deleteReport(reportId);

        if (success) {
            Toast.makeText(this, "Reporte eliminado correctamente", Toast.LENGTH_SHORT).show();
            finish(); // Volver a la actividad anterior
        } else {
            Toast.makeText(this, "Error al eliminar el reporte", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}
