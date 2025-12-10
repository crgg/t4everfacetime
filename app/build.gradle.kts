plugins {
    alias(libs.plugins.android.application)
    id ("com.google.dagger.hilt.android")
}

android {
    namespace = "com.t4app.videocalltest"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.t4app.videocalltest"
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

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.okhttp3.okhttp)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation("com.squareup.retrofit2:adapter-rxjava3:3.0.0")
    implementation(libs.logging.interceptor)

    implementation(libs.android)

    implementation("com.google.dagger:hilt-android:2.48")
    annotationProcessor ("com.google.dagger:hilt-android-compiler:2.48")


    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}