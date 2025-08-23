package com.example.androidapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.androidapp.R;
import com.example.androidapp.database.DatabaseHelper;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AddReportActivity extends AppCompatActivity {
    private static final int MAX_IMAGES = 4;

    private static final String TAG = "ReportActivity";
    private RatingBar ratingBar;
    private EditText etLugar, etExplicacion;
    private Button btnTomarFoto, btnEnviar;
    private LinearLayout imageContainer;
    private ArrayList<String> imagePaths = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private TextView tvImageCounter;

    // Códigos para manejo de permisos y cámara
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int REQUEST_IMAGE_GALLERY = 101;

    private String currentPhotoPath;

    // Variables para anuncios
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_report);

        // Inicializar AdMob
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        // Cargar banner
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Cargar anuncio intersticial
        loadInterstitialAd();

        // Inicializar base de datos
        dbHelper = new DatabaseHelper(this);

        // Inicializar vistas
        ratingBar = findViewById(R.id.ratingBar);
        etLugar = findViewById(R.id.etLugar);
        etExplicacion = findViewById(R.id.etExplicacion);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnEnviar = findViewById(R.id.btnEnviar);
        imageContainer = findViewById(R.id.imageContainer);
        tvImageCounter = findViewById(R.id.tvImageCounter);
        updateImageCounter();

        // Configurar listeners
        btnTomarFoto.setOnClickListener(v -> showImagePickerDialog());
        btnEnviar.setOnClickListener(v -> {
            if (validarFormulario()) {
                // Mostrar anuncio antes de guardar
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(AddReportActivity.this);
                    // Guardar después de mostrar el anuncio
                    mInterstitialAd.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Este método se llama cuando el usuario cierra el anuncio
                            enviarReporte();
                            finish();
                        }
                    });
                } else {
                    // Si no hay anuncio, guardar directamente
                    enviarReporte();
                    finish();
                }
            }
        });
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, "ca-app-pub-3990292709910452~9205783398", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError loadAdError) {
                        mInterstitialAd = null;
                        Log.d(TAG, "Error loading interstitial ad: " + loadAdError.getMessage());
                    }
                });
    }

    private void showImagePickerDialog() {
        if (imagePaths.size() >= MAX_IMAGES) {
            Toast.makeText(this, "Máximo " + MAX_IMAGES + " imágenes permitidas", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar imagen");
        builder.setItems(new CharSequence[]{"Tomar foto", "Elegir de galería"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    checkCameraPermission();
                    break;
                case 1:
                    checkStoragePermission();
                    break;
            }
        });
        builder.show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
            } else {
                dispatchPickFromGalleryIntent();
            }
        } else {
            // Android < 13
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                dispatchPickFromGalleryIntent();
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error al crear el archivo", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.androidapp.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchPickFromGalleryIntent() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                // Imagen capturada con la cámara - usar currentPhotoPath directamente
                if (currentPhotoPath != null) {
                    imagePaths.add(currentPhotoPath);
                    addImageThumbnail(currentPhotoPath);
                    currentPhotoPath = null; // Resetear para la próxima foto
                }
            } else if (requestCode == REQUEST_IMAGE_GALLERY) {
                // Imagen seleccionada de la galería
                if (data != null && data.getData() != null) {
                    Uri selectedImageUri = data.getData();
                    try {
                        // Obtener la ruta real del archivo
                        String imagePath = getRealPathFromURI(selectedImageUri);
                        if (imagePath != null) {
                            imagePaths.add(imagePath);
                            addImageThumbnail(imagePath);
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error al obtener ruta de la imagen", e);
                    }
                }
            }
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        android.database.Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
    }

    private void addImageThumbnail(String imagePath) {
        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
        params.setMargins(0, 0, 16, 0);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Usar Picasso para cargar la miniatura
        Picasso.get().load(new File(imagePath))
                .resize(200, 200)
                .centerCrop()
                .into(imageView);

        // Agregar funcionalidad para eliminar imagen
        imageView.setOnClickListener(v -> {
            int position = imageContainer.indexOfChild(v);
            if (position != -1) {
                imagePaths.remove(position);
                imageContainer.removeViewAt(position);
                updateImageCounter();
            }
        });

        imageContainer.addView(imageView);
        updateImageCounter();
    }

    private void updateImageCounter() {
        String counterText = imagePaths.size() + "/4 imágenes";
        tvImageCounter.setText(counterText);

        if (imagePaths.size() >= 4) {
            tvImageCounter.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            btnTomarFoto.setEnabled(false);
        } else {
            tvImageCounter.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            btnTomarFoto.setEnabled(true);
        }
    }

    private void enviarReporte() {
        String lugar = etLugar.getText().toString().trim();
        String explicacion = etExplicacion.getText().toString().trim();
        int calificacion = (int) ratingBar.getRating();
        int userId = getUserId();

        // Construir cadena de rutas de imágenes
        StringBuilder imagePathsBuilder = new StringBuilder();
        for (String path : imagePaths) {
            if (imagePathsBuilder.length() > 0) {
                imagePathsBuilder.append(",");
            }
            imagePathsBuilder.append(path);
        }

        String imagePathsString = imagePathsBuilder.toString();

        // Guardar reporte en base de datos local
        long reportId = dbHelper.addReport(userId, lugar, calificacion, explicacion, imagePathsString);
        if (reportId != -1) {
            Toast.makeText(this, "Reporte guardado localmente", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error al guardar. Verifique logs.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validarFormulario() {
        String lugar = etLugar.getText().toString().trim();
        String explicacion = etExplicacion.getText().toString().trim();

        if (lugar.isEmpty() || explicacion.isEmpty()) {
            Toast.makeText(this, "Lugar y explicación son obligatorios", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (imagePaths.size() == 0) {
            Toast.makeText(this, "Debe tomar al menos una foto", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (getUserId() == -1) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private int getUserId() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        return prefs.getInt("user_id", -1);
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
