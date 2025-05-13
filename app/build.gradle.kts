plugins {
    id ("com.android.application")
    id ("org.jetbrains.kotlin.android")
    id ("com.google.gms.google-services")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.sportapplication"
    compileSdk = 35

    buildFeatures {
        viewBinding{
            enable = true
        }
        buildConfig = true
    }

    repositories {
    }

    defaultConfig {
        applicationId = "com.example.sportapplication"
        minSdk = 26
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.ui)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation (libs.circleimageview) // Библиотека для отображения изображений

    // Используя Firebase Android BoM , ваше приложение всегда будет использовать совместимые версии библиотек Firebase Android
    implementation(platform(libs.firebase.bom))
    implementation(libs.play.services.auth)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.googleid)

    implementation (libs.googleid)

    // DataStore для хранения пар "ключ-значение"
    implementation (libs.androidx.datastore.preferences)
    // Firebase: аутентификация и Realtime Database с поддержкой Kotlin
    implementation (libs.firebase.database.ktx)
    // AndroidX Lifecycle для использования lifecycleScope
    implementation (libs.androidx.lifecycle.runtime.ktx)
    // Kotlin Coroutines для Android
    implementation (libs.kotlinx.coroutines.android)
    // Work Manager
    implementation (libs.androidx.work.runtime.ktx)
    // Credential Manager от Google для аутентификации
    implementation (libs.androidx.credentials.v150)
    implementation (libs.androidx.credentials.play.services.auth.v150)
    // Google Play Services Location
    implementation (libs.material.v190)
    implementation (libs.play.services.maps)
    implementation (libs.play.services.location)
    // Yandex Maps Android SDK
    implementation (libs.maps.mobile)

    // 🔹 Инструментальные тесты (Espresso)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation (libs.androidx.rules)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(libs.androidx.espresso.contrib)

    // 🔹 Unit-тестирование (JUnit + Mockito)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.androidx.core.testing) // Для тестирования ViewModel
    androidTestImplementation (libs.androidx.fragment.testing)  // Для тестирования фрагментов
    androidTestImplementation (libs.androidx.uiautomator)

    implementation (libs.androidx.recyclerview)
    implementation (libs.androidx.cardview)

    implementation (libs.mpandroidchart) // Библиотека для отображения графиков





}
