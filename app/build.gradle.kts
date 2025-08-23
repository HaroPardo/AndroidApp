plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.androidapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.androidapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Dependencias principales
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Nuevas dependencias para tu proyecto
    implementation("com.android.volley:volley:1.2.1")  // Para conexión HTTP
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation(libs.core.ktx)

    // Dependencias de prueba
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Servicios de ubicación (mantén solo location, no maps ya que usas OSMDroid)
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Dependencias de OSMDroid CORREGIDAS
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("org.osmdroid:osmdroid-android:6.1.16")
    implementation("com.google.android.gms:play-services-ads:22.6.0")
}
