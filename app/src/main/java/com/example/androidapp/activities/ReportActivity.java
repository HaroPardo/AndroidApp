package com.example.androidapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.androidapp.R;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    RatingBar ratingBar;
    EditText etLugar, etExplicacion;
    Button btnTomarFoto, btnEnviar;
    LinearLayout imageContainer;
    ArrayList<Bitmap> imagenes = new ArrayList<>();

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

        btnTomarFoto.setOnClickListener(v -> abrirCamara());
        btnEnviar.setOnClickListener(v -> enviarReporte());
    }

    private void abrirCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    100);
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamara();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100 && resultCode == RESULT_OK){
            Bitmap image = (Bitmap) data.getExtras().get("data");
            imagenes.add(image);

            // Mostrar miniatura
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(image);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
            imageContainer.addView(imageView);
        }
    }

    private void enviarReporte() {
        String url = "http://10.0.2.2/android_backend/upload_report.php";
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);

        // Convertir imágenes a Base64
        ArrayList<String> imagenesBase64 = new ArrayList<>();
        for(Bitmap image : imagenes){
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageBytes = baos.toByteArray();
            imagenesBase64.add(Base64.encodeToString(imageBytes, Base64.DEFAULT));
        }

        JSONArray jsonImages = new JSONArray(imagenesBase64);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (response.equals("success")) {
                        Toast.makeText(ReportActivity.this, "Reporte enviado!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ReportActivity.this, "Error: " + response, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(ReportActivity.this, "Error al enviar: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("usuario_id", String.valueOf(userId));
                params.put("lugar", etLugar.getText().toString());
                params.put("calificacion", String.valueOf((int) ratingBar.getRating()));
                params.put("explicacion", etExplicacion.getText().toString());
                params.put("imagenes", jsonImages.toString());
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }
}