package com.example.androidapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.androidapp.R;
import com.example.androidapp.database.DatabaseHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class ReportActivity extends AppCompatActivity {

    private static final String TAG = "ReportActivity";
    private RatingBar ratingBar;
    private EditText etLugar, etExplicacion;
    private Button btnTomarFoto, btnEnviar;
    private LinearLayout imageContainer;
    private ArrayList<Bitmap> imagenes = new ArrayList<>();
    private DatabaseHelper dbHelper;

    // Códigos para manejo de permisos y cámara
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int CAMERA_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        // Inicializar base de datos
        dbHelper = new DatabaseHelper(this);

        // Inicializar vistas
        ratingBar = findViewById(R.id.ratingBar);
        etLugar = findViewById(R.id.etLugar);
        etExplicacion = findViewById(R.id.etExplicacion);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnEnviar = findViewById(R.id.btnEnviar);
        imageContainer = findViewById(R.id.imageContainer);

        // Configurar listeners
        btnTomarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissions();
            }
        });

        btnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarReporte();
            }
        });
    }

    private void checkPermissions() {
        // Lista de permisos a verificar
        ArrayList<String> permissionsNeeded = new ArrayList<>();

        // Permiso de cámara
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        // Permisos para imágenes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Android < 13
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        // Si hay permisos pendientes, solicitarlos
        if (!permissionsNeeded.isEmpty()) {
            // Convertir ArrayList a array de Strings
            String[] permissionsArray = new String[permissionsNeeded.size()];
            permissionsArray = permissionsNeeded.toArray(permissionsArray);

            ActivityCompat.requestPermissions(
                    this,
                    permissionsArray,
                    PERMISSION_REQUEST_CODE
            );
        } else {
            abrirCamara();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                abrirCamara();
            } else {
                showToast("Permisos necesarios para usar la cámara");
            }
        }
    }

    private void abrirCamara() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        } else {
            showToast("No se encontró una aplicación de cámara");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap image = (Bitmap) extras.get("data");
                if (image != null) {
                    imagenes.add(image);
                    addImageThumbnail(image);
                }
            }
        }
    }

    private void addImageThumbnail(Bitmap image) {
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(image);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        imageContainer.addView(imageView);
    }

    private void enviarReporte() {
        if (!validarFormulario()) return;

        // Obtener datos del formulario
        String lugar = etLugar.getText().toString().trim();
        String explicacion = etExplicacion.getText().toString().trim();
        int calificacion = (int) ratingBar.getRating();
        int userId = getUserId();

        // Guardar imágenes y obtener rutas
        StringBuilder imagePaths = new StringBuilder();
        for (int i = 0; i < imagenes.size(); i++) {
            String fileName = "report_" + System.currentTimeMillis() + "_" + i + ".jpg";
            String path = guardarImagen(imagenes.get(i), fileName);

            if (path != null) {
                imagePaths.append(path).append(",");
            }
        }

        if (imagePaths.length() == 0) {
            showToast("Error al guardar imágenes");
            return;
        }

        // Quitar la última coma
        String rutasImagenes = imagePaths.substring(0, imagePaths.length() - 1);

        // Guardar reporte en base de datos local
        long reportId = dbHelper.addReport(userId, lugar, calificacion, explicacion, rutasImagenes);

        if (reportId != -1) {
            showToast("Reporte guardado localmente");
            finish();
        } else {
            showToast("Error al guardar reporte");
        }
    }

    private boolean validarFormulario() {
        String lugar = etLugar.getText().toString().trim();
        String explicacion = etExplicacion.getText().toString().trim();

        if (lugar.isEmpty() || explicacion.isEmpty()) {
            showToast("Lugar y explicación son obligatorios");
            return false;
        }

        if (imagenes.size() == 0) {  // Usar size() en lugar de isEmpty()
            showToast("Debe tomar al menos una foto");
            return false;
        }

        if (getUserId() == -1) {
            showToast("Usuario no identificado");
            return false;
        }

        return true;
    }

    private int getUserId() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        return prefs.getInt("user_id", -1);
    }

    private String guardarImagen(Bitmap bitmap, String fileName) {
        try {
            // Crear archivo en almacenamiento interno
            File file = new File(getFilesDir(), fileName);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error guardando imagen", e);
            return null;
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
