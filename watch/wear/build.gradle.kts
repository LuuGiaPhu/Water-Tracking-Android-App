plugins {
//    id("com.android.application")
//    id("org.jetbrains.kotlin.android") version "1.8.0"
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35
    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/LICENSE.md")
        exclude("META-INF/NOTICE.md")
    }
    defaultConfig {
        applicationId = "com.example.myapplication"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.android.gms:play-services-wearable:17.0.0")
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.wear:wear-input:1.1.0")
    implementation("androidx.wear:wear-input-testing:1.1.0")
    implementation("androidx.wear:wear-ongoing:1.0.0")
    implementation("androidx.wear:wear-phone-interactions:1.0.1")
    implementation("androidx.wear:wear-remote-interactions:1.0.0")
    implementation ("com.google.android.gms:play-services-wearable:18.0.0")
    implementation ("com.google.android.gms:play-services-base:18.0.1")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.google.android.gms:play-services-auth:20.0.0")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.google.android.gms:play-services-wearable:18.0.0")
    implementation("com.google.android.gms:play-services-base:18.0.1")
    implementation("com.google.api-client:google-api-client:1.32.1")
    implementation("com.google.api-client:google-api-client-android:1.32.1")
    implementation("com.google.apis:google-api-services-gmail:v1-rev20211010-1.32.1")
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    implementation("com.google.apis:google-api-services-gmail:v1-rev29-1.20.0")
    implementation("com.google.firebase:firebase-database:20.0.5")
    implementation("com.google.firebase:firebase-firestore:24.0.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("com.google.api-client:google-api-client-gson:1.32.1")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    // Firebase BOM for version management
    implementation(platform("com.google.firebase:firebase-bom:32.2.3"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("com.google.android.gms:play-services-fitness:21.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("androidx.health.connect:connect-client:1.1.0-alpha05")
}
apply(plugin = "com.android.application")
apply(plugin = "com.google.gms.google-services")