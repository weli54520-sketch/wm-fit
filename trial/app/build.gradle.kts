plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.waleed.launcher.trial"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.waleed.launcher.trial"
        minSdk = 30
        targetSdk = 35
        // رقم التحديث: كل تحديث نزيد versionCode واحد ونغير versionName
        versionCode = 8
        versionName = "0.8"
    }

    // توقيع ثابت عشان كل تحديث ينزل فوق القديم بدون ما يضيع شي
    signingConfigs {
        create("trial") {
            storeFile = file("trial.keystore")
            storePassword = "waleedtrial"
            keyAlias = "trial"
            keyPassword = "waleedtrial"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("trial")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.core:core-ktx:1.13.1")
    // حساب مواقيت الصلاة بطريقة أم القرى (بدون إنترنت)
    implementation("com.batoulapps.adhan:adhan:1.2.1")
}
