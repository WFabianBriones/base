plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.uleammed"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.uleammed"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ⭐ BuildConfig fields
        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("boolean", "DEBUG", "true")

        // ⭐ NUEVO: Para TensorFlow Lite en dispositivos con diferentes arquitecturas
        ndk {
            abiFilters += listOf("arm64-v8a")
            //abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "DEBUG", "false")
        }
        debug {
            buildConfigField("boolean", "DEBUG", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true  // ⭐ Para usar BuildConfig
        mlModelBinding = true  // ⭐ Para TensorFlow Lite
    }

    // ⭐ Configuración de Compose Compiler
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    // ⭐ No comprimir archivos .tflite
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // ⭐ Configuración para archivos .tflite
    aaptOptions {
        noCompress("tflite")
        noCompress("lite")
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // ⭐ Material Components para Android (necesario para temas Material3 en XML)
    implementation("com.google.android.material:material:1.11.0")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Compose adicionales para ViewModel y Navigation
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ⭐ Credential Manager (para Google Sign In)
    implementation("androidx.credentials:credentials:1.2.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    // ⭐ WorkManager (para notificaciones programadas)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // ⭐ Gson (para serialización JSON)
    implementation("com.google.code.gson:gson:2.10.1")

    // ⭐ TensorFlow Lite - PARA EL MODELO DE IA
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // OPCIONAL: TensorFlow Lite GPU (mejor rendimiento)
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")

    // OPCIONAL: Select TF Ops (si usas operadores de TensorFlow)
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")

    // Coroutines (para operaciones asíncronas)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    //implementation("io.github.ehsannarmani:compose-charts:0.1.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}