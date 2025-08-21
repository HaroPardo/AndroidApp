package com.example.androidapp.activities;

import android.app.Dialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
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

import java.io.File;

public class ReportDetailActivity extends AppCompatActivity {

    private TextView tvPlace, tvExplanation;
    private RatingBar ratingBar;
    private LinearLayout imageContainer;
    private DatabaseHelper dbHelper;

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
        long reportId = getIntent().getLongExtra("REPORT_ID", -1);

        if (reportId == -1) {
            Toast.makeText(this, "Error: ID de reporte no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
            Log.e("ReportDetail", "Error cargando detalles: " + e.getMessage());
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
                    // Reducir el tamaño de la imagen para evitar problemas de memoria
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4; // Reducir tamaño 4 veces

                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                    if (bitmap != null) {
                        ImageView imageView = new ImageView(this);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                400, 400
                        );
                        params.setMargins(0, 0, 16, 0);
                        imageView.setLayoutParams(params);
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        imageView.setImageBitmap(bitmap);

                        // Agregar funcionalidad para ampliar imagen al hacer clic
                        imageView.setOnClickListener(v -> {
                            showFullScreenImage(imgFile.getAbsolutePath());
                        });

                        imageContainer.addView(imageView);
                    }
                }
            } catch (Exception e) {
                Log.e("ReportDetail", "Error cargando imagen: " + path, e);
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

        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                fullScreenImageView.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            Log.e("ReportDetail", "Error mostrando imagen completa", e);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}