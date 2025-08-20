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

import com.example.androidapp.R;
import com.example.androidapp.database.DatabaseHelper;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private EditText etNombre, etEmail, etPassword;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicializar base de datos
        dbHelper = new DatabaseHelper(this);

        // Inicializar vistas
        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvLogin = findViewById(R.id.tvLogin); // 🔹 Agregado para volver a Login

        // Listeners
        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> finish()); // 🔹 Volver a Login (cierra RegisterActivity)
    }

    private void registerUser() {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showToast("Todos los campos son obligatorios");
            return;
        }

        long userId = dbHelper.addUser(nombre, email, password);

        if (userId != -1) {
            saveUserSession((int) userId, nombre);
            showToast("Registro exitoso");
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        } else {
            showToast("Error al registrar usuario (¿email ya existe?)");
        }
    }

    private void saveUserSession(int userId, String name) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        prefs.edit()
                .putInt("user_id", userId)
                .putString("nombre", name)
                .putBoolean("is_logged_in", true) // 🔹 coherencia con LoginActivity
                .apply();
        Log.d(TAG, "Usuario registrado y sesión iniciada: " + name);
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
