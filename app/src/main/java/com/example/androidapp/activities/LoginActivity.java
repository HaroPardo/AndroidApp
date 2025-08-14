package com.example.androidapp.activities;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.androidapp.R;
import com.example.androidapp.network.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Verificar si ya hay una sesión activa
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        if (prefs.contains("user_id")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validar campos
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email y contraseña son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        // Usa tu IP de Kubuntu (actualiza según tu configuración)
        String url = "http://192.168.1.144/app_android/login.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                this::handleLoginResponse,
                this::handleLoginError
        ) {
            @Override
            public byte[] getBody() {
                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put("email", email);
                    jsonBody.put("password", password);
                    return jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating JSON body", e);
                    return null;
                }
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        VolleySingleton.getInstance(this).getRequestQueue().add(request);
    }

    private void handleLoginResponse(String response) {
        try {
            Log.d(TAG, "Login response: " + response);
            JSONObject json = new JSONObject(response);

            if (json.has("status")) {
                String status = json.getString("status");

                if (status.equals("success")) {
                    int userId = json.getInt("user_id");
                    String nombre = json.getString("nombre");

                    // Guardar en SharedPreferences
                    saveUserSession(userId, nombre);

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else if (json.has("message")) {
                    handleLoginErrorResponse(json.getString("message"));
                } else {
                    Toast.makeText(this, "Error desconocido en el servidor", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Respuesta inválida del servidor", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error: " + e.getMessage());
            Toast.makeText(this, "Error procesando respuesta: " + response, Toast.LENGTH_LONG).show();
        }
    }

    private void saveUserSession(int userId, String nombre) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        prefs.edit()
                .putInt("user_id", userId)
                .putString("nombre", nombre)
                .putBoolean("is_logged_in", true)
                .apply();
    }

    private void handleLoginErrorResponse(String errorMsg) {
        if (errorMsg.equals("invalid_password")) {
            Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
            etPassword.requestFocus();
        } else if (errorMsg.equals("user_not_found")) {
            Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
        } else {
            Toast.makeText(this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLoginError(VolleyError error) {
        String errorMessage = "Error de conexión";

        if (error instanceof TimeoutError) {
            errorMessage = "Tiempo de espera agotado";
        } else if (error instanceof NoConnectionError) {
            errorMessage = "Sin conexión a internet";
        } else if (error instanceof AuthFailureError) {
            errorMessage = "Error de autenticación";
        } else if (error instanceof ServerError) {
            errorMessage = "Error en el servidor";
        } else if (error instanceof NetworkError) {
            errorMessage = "Error de red";
        } else if (error instanceof ParseError) {
            errorMessage = "Error en el formato de respuesta";
        }

        // Intentar obtener respuesta del servidor si existe
        if (error.networkResponse != null && error.networkResponse.data != null) {
            String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
            try {
                JSONObject json = new JSONObject(responseBody);
                if (json.has("message")) {
                    errorMessage += ": " + json.getString("message");
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing error response", e);
                errorMessage += ": " + responseBody;
            }
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Volley error: " + error.getMessage(), error);
    }
}
