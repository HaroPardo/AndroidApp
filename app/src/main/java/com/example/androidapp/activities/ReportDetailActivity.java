package com.example.androidapp.activities;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

    // Vistas como variables de clase
    private TextView tvPlace;
    private RatingBar ratingBar;
    private TextView tvExplanation;
    private LinearLayout imageContainer;
    private View imageSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        // Inicializar vistas
        tvPlace = findViewById(R.id.tvPlace);
        ratingBar = findViewById(R.id.ratingBar);
        tvExplanation = findViewById(R.id.tvExplanation);
        imageContainer = findViewById(R.id.imageContainer);
        imageSection = findViewById(R.id.imageSection);

        long reportId = getIntent().getLongExtra("report_id", -1);

        if (reportId == -1) {
            Toast.makeText(this, "Reporte no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        Cursor cursor = null;

        try {
            cursor = dbHelper.getReportDetails(reportId);

            if (cursor != null && cursor.moveToFirst()) {
                String place = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PLACE));
                int rating = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_RATING));
                String explanation = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EXPLANATION));
                String images = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IMAGES));

                // Mostrar datos
                tvPlace.setText(place);
                ratingBar.setRating(rating);
                tvExplanation.setText(explanation);

                // Mostrar imágenes si existen
                if (images != null && !images.isEmpty()) {
                    String[] imagePaths = images.split(",");
                    for (String path : imagePaths) {
                        addImageToContainer(imageContainer, path);
                    }
                    imageSection.setVisibility(View.VISIBLE);
                } else {
                    imageSection.setVisibility(View.GONE);
                }
            } else {
                Toast.makeText(this, "Reporte no encontrado", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e("ReportDetail", "Error mostrando reporte: " + e.getMessage());
            Toast.makeText(this, "Error cargando reporte", Toast.LENGTH_SHORT).show();
            finish();
        } finally {
            if (cursor != null) cursor.close();
            dbHelper.close();
        }
    }

    private void addImageToContainer(LinearLayout container, String imagePath) {
        try {
            // Reducir calidad de imagen
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;

            // Verificar que el archivo existe
            File imgFile = new File(imagePath);
            if (!imgFile.exists()) return;

            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
            if (bitmap == null) return;

            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(400, 400);
            params.setMargins(0, 0, 16, 0);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageBitmap(bitmap);

            container.addView(imageView);
        } catch (OutOfMemoryError e) {
            Log.e("ReportDetail", "Memoria insuficiente para cargar imagen");
        }
    }
}
