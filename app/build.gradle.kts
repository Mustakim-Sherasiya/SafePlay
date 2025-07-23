plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
//    id("com.android.application")
    id("com.google.gms.google-services")

}

android {
    namespace = "com.chat.safeplay"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chat.safeplay"
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")                 // Core KTX
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1") // Lifecycle KTX
    implementation("androidx.activity:activity-compose:1.9.0") // Compose Activity


    implementation("androidx.activity:activity-ktx:1.7.2")

    implementation(platform("androidx.compose:compose-bom:2023.06.01")) // Compose BOM for consistent versions
    implementation("androidx.compose.ui:ui")                         // Compose UI core
    implementation("androidx.compose.ui:ui-graphics")                // Compose UI graphics
    implementation("androidx.compose.ui:ui-tooling-preview")         // Preview support

    implementation("androidx.compose.material3:material3:1.2.0")     // Latest stable Material3
    implementation("androidx.compose.material3:material3-window-size-class:1.2.0") // Optional window size helper

    implementation("androidx.navigation:navigation-compose:2.7.2")   // Navigation Compose

    implementation("androidx.compose.material:material-icons-extended:1.4.3") // Material Icons Extended

    implementation(platform("com.google.firebase:firebase-bom:33.16.0")) // Firebase BOM
    implementation("com.google.firebase:firebase-analytics-ktx")       // Firebase Analytics
    implementation("com.google.firebase:firebase-auth-ktx")            // Firebase Auth
    implementation("com.google.firebase:firebase-firestore-ktx:24.7.1") // Firebase Firestore

    testImplementation("junit:junit:4.13.2")                          // JUnit
    androidTestImplementation("androidx.test.ext:junit:1.1.5")        // AndroidX JUnit
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // Espresso
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.06.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")  // Compose UI testing

    debugImplementation("androidx.compose.ui:ui-tooling")             // Compose tooling debug
    debugImplementation("androidx.compose.ui:ui-test-manifest")       // Compose test manifest


}








//dependencies {
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.activity.compose)
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.ui)
//    implementation(libs.androidx.ui.graphics)
//    implementation(libs.androidx.ui.tooling.preview)
//    implementation(libs.androidx.material3)           // Make sure libs.androidx.material3 has correct version
//    implementation("androidx.navigation:navigation-compose:2.7.2")
//    // replace navigation.runtime.android with navigation.compose
//
//    implementation(libs.androidx.compose.material.icons.extended)  // icons extended
//
//    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
//    implementation("com.google.firebase:firebase-analytics")
//    implementation("com.google.firebase:firebase-auth-ktx")
//    implementation("androidx.compose.material3:material3:1.1.0") // Replace 1.1.0 with latest stable version
//    implementation("com.google.firebase:firebase-firestore-ktx:24.7.1")
//
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.ui.test.junit4)
//    debugImplementation(libs.androidx.ui.tooling)
//    debugImplementation(libs.androidx.ui.test.manifest)
//}
