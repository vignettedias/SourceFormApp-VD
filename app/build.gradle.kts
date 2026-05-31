plugins {

    id("com.android.application")

    id("org.jetbrains.kotlin.android")

    id("com.google.gms.google-services")
}

android {

    namespace =
        "com.example.sourceformapp"

    compileSdk = 36

    defaultConfig {

        applicationId =
            "com.example.sourceformapp"

        minSdk = 24

        targetSdk = 36

        versionCode = 1

        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(

                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),

                "proguard-rules.pro"
            )
        }
    }

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }

    kotlinOptions {

        jvmTarget = "11"
    }

    buildFeatures {

        buildConfig = true
    }
}

dependencies {

    implementation(
        "androidx.core:core-ktx:1.13.1"
    )

    implementation(
        "androidx.appcompat:appcompat:1.7.0"
    )

    implementation(
        "androidx.constraintlayout:constraintlayout:2.1.4"
    )

    implementation(
        "androidx.activity:activity-ktx:1.9.0"
    )

    implementation(
        "com.google.android.material:material:1.12.0"
    )

    // QR Scanner

    implementation(
        "com.journeyapps:zxing-android-embedded:4.3.0"
    )

    implementation(
        "com.google.zxing:core:3.5.3"
    )

    // Retrofit

    implementation(
        "com.squareup.retrofit2:retrofit:2.11.0"
    )

    implementation(
        "com.squareup.retrofit2:converter-gson:2.11.0"
    )

    implementation(
        "com.squareup.okhttp3:logging-interceptor:4.12.0"
    )

    // Coroutines

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1"
    )

    // Firebase

    implementation(
        platform(
            "com.google.firebase:firebase-bom:33.1.2"
        )
    )

    implementation(
        "com.google.firebase:firebase-auth-ktx"
    )

    implementation(
        "com.google.firebase:firebase-firestore-ktx"
    )

    implementation(
        "com.google.firebase:firebase-storage-ktx"
    )

    // Google Auth

    implementation(
        "com.google.android.gms:play-services-auth:21.2.0"
    )

    // Facebook SDK

    implementation(
        "com.facebook.android:facebook-android-sdk:17.0.2"
    )

    // Tests

    testImplementation(
        "junit:junit:4.13.2"
    )

    androidTestImplementation(
        "androidx.test.ext:junit:1.2.1"
    )

    androidTestImplementation(
        "androidx.test.espresso:espresso-core:3.6.1"
    )
}
