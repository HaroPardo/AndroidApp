package com.example.androidapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.androidapp.R;

import org.osmdroid.config.Configuration;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configurar OSMDroid
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // Establecer la ruta de caché para los tiles
        File basePath = new File(getCacheDir(), "osmdroid");
        if (!basePath.exists()) {
            basePath.mkdirs();
        }
        Configuration.getInstance().setOsmdroidBasePath(basePath);

        File tileCache = new File(basePath, "tiles");
        if (!tileCache.exists()) {
            tileCache.mkdirs();
        }
        Configuration.getInstance().setOsmdroidTileCache(tileCache);

        setContentView(R.layout.activity_main);

        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnGoToReports = findViewById(R.id.btnGoToReports);
        Button btnGoToReminders = findViewById(R.id.btnGoToReminders);

        btnGoToReports.setOnClickListener(v -> {
            startActivity(new Intent(this, ViewReportsActivity.class));
        });

        btnGoToReminders.setOnClickListener(v -> {
            startActivity(new Intent(this, RemindersActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            // Limpiar sesión
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            prefs.edit().clear().apply();

            // Redirigir a login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_reminders) {
            startActivity(new Intent(this, RemindersActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}