plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.scolarai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.scolarai"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig fields go in defaultConfig
        buildConfigField("String", "GEMINI_API_KEY", "\"AIzaSyBHcDbHDUs7YX-XL4T5jysp4njh7a5TEF0\"")
    }

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.legacy.support.v4)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}