plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "cleanmail.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "cleanmail.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // KMP shared модуль
    implementation(project(":shared"))

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // WorkManager — экономный режим уведомлений
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // DataStore — настройки приложения
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Security — KeyStore обёртка
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Coroutines Android
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // SQLCipher
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
