package com.example.androidapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
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

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.androidapp.R;
import com.example.androidapp.network.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    RatingBar ratingBar;
    EditText etLugar, etExplicacion;
    Button btnTomarFoto, btnEnviar;
    LinearLayout imageContainer;
    ArrayList<Bitmap> imagenes = new ArrayList<>();

    // Códigos para manejo de permisos y cámara
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int CAMERA_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        // Inicializar vistas
        ratingBar = findViewById(R.id.ratingBar);
        etLugar = findViewById(R.id.etLugar);
        etExplicacion = findViewById(R.id.etExplicacion);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnEnviar = findViewById(R.id.btnEnviar);
        imageContainer = findViewById(R.id.imageContainer);

        // Configurar listeners
        btnTomarFoto.setOnClickListener(v -> checkPermissions());
        btnEnviar.setOnClickListener(v -> enviarReporte());
    }

    /**
     * Verifica y solicita los permisos necesarios para usar la cámara
     */
    private void checkPermissions() {
        // Lista de permisos a verificar
        List<String> permissionsNeeded = new ArrayList<>();

        // Permiso de cámara siempre necesario
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        // Permisos para imágenes dependiendo de la versión de Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requiere permiso especial para imágenes
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Android < 13 usa permiso de almacenamiento
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        // Si hay permisos pendientes, solicitarlos
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE
            );
        } else {
            // Todos los permisos están concedidos, abrir cámara
            abrirCamara();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Verificar si todos los permisos fueron concedidos
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
                Toast.makeText(this, "Permisos necesarios para usar la cámara", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Abre la cámara para tomar una foto
     */
    private void abrirCamara() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(this, "No se encontró una aplicación de cámara", Toast.LENGTH_SHORT).show();
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

                    // Mostrar miniatura
                    ImageView imageView = new ImageView(this);
                    imageView.setImageBitmap(image);
                    imageView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
                    imageContainer.addView(imageView);
                }
            }
        }
    }

    /**
     * Envía el reporte al servidor
     */
    private void enviarReporte() {
        String url = "http://192.168.1.144/app_android/upload_report.php";
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);

        if (userId == -1) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar campos obligatorios
        String lugar = etLugar.getText().toString().trim();
        String explicacion = etExplicacion.getText().toString().trim();

        if (lugar.isEmpty() || explicacion.isEmpty()) {
            Toast.makeText(this, "Lugar y explicación son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convertir imágenes a Base64
        JSONArray jsonImages = new JSONArray();
        for (Bitmap image : imagenes) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                jsonImages.put(Base64.encodeToString(imageBytes, Base64.DEFAULT));
            } catch (Exception e) {
                Log.e("ReportActivity", "Error procesando imagen", e);
            }
        }

        // Crear cuerpo de la petición
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("usuario_id", userId);
            jsonBody.put("lugar", lugar);
            jsonBody.put("calificacion", (int) ratingBar.getRating());
            jsonBody.put("explicacion", explicacion);
            jsonBody.put("imagenes", jsonImages);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creando reporte", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear petición HTTP
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
                response -> {
                    try {
                        String status = response.getString("status");
                        if ("success".equals(status)) {
                            Toast.makeText(ReportActivity.this, "Reporte enviado!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            String errorMsg = response.optString("message", "Error desconocido");
                            Toast.makeText(ReportActivity.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(ReportActivity.this, "Error en respuesta: " + response, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String errorMsg = "Error de conexión";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMsg = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                    }
                    Toast.makeText(ReportActivity.this, "Error al enviar: " + errorMsg, Toast.LENGTH_LONG).show();
                    Log.e("ReportActivity", "Error en petición: " + errorMsg, error);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };

        // Añadir petición a la cola
        VolleySingleton.getInstance(this).getRequestQueue().add(request);
    }
}
