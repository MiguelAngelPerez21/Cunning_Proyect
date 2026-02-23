import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    // Plugin de Google Services activado
    id("com.google.gms.google-services")
}

// 1. CARGAMOS EL ARCHIVO local.properties EN SECRETO
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
// Leemos la clave (si no existe, ponemos cadena vacía para que no crashee)
val geminiApiKey = localProperties.getProperty("geminiApiKey") ?: ""

android {
    namespace = "com.example.cunning_proyect"
    compileSdk = 35 // Subimos a 35 (Estable Android 15)

    defaultConfig {
        applicationId = "com.example.cunning_proyect"
        minSdk = 24
        targetSdk = 35 // Coincide con compileSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 2. INYECTAMOS LA CLAVE EN LA CLASE BuildConfig
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    // 3. ACTIVAMOS LA GENERACIÓN DE BUILDCONFIG
    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // --- LIBRERÍAS NÚCLEO (Versiones forzadas para evitar error de SDK 36) ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.0") // Esta es la que daba error, la bajamos a la estable
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(libs.firebase.firestore)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // --- TUS DEPENDENCIAS EXTRAS ---

    // Mapas
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Imagen Circular
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Navegación
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    // Recycler View (Para las listas)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // --- FIREBASE (Hito 3) ---
    // Importar la BOM (gestiona las versiones automáticamente)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

    // Librerías de Firebase (sin poner versión, la BOM lo gestiona)
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")      // Login
    implementation("com.google.firebase:firebase-firestore") // Base de Datos
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.firebase:firebase-firestore:24.10.0")
    // Almacenamiento (Para las FOTOS de las incidencias)
    implementation("com.google.firebase:firebase-storage:20.3.0")
    // Autenticación (Para saber quién es quién)
    implementation("com.google.firebase:firebase-auth:22.3.0")
    implementation("com.google.firebase:firebase-auth-ktx:23.1.0")
}