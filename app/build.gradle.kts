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



    implementation("androidx.compose.material:material-icons-extended:1.4.3") // Material Icons Extended

    implementation(platform("com.google.firebase:firebase-bom:33.16.0")) // Firebase BOM
    implementation("com.google.firebase:firebase-analytics-ktx")       // Firebase Analytics
    implementation("com.google.firebase:firebase-auth-ktx")            // Firebase Auth
    implementation("com.google.firebase:firebase-firestore-ktx:24.7.1")
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.bom)
    implementation(libs.androidx.room.ktx) // Firebase Firestore

    testImplementation("junit:junit:4.13.2")                          // JUnit
    androidTestImplementation("androidx.test.ext:junit:1.1.5")        // AndroidX JUnit
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // Espresso
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.06.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")  // Compose UI testing

    debugImplementation("androidx.compose.ui:ui-tooling")             // Compose tooling debug
    debugImplementation("androidx.compose.ui:ui-test-manifest")       // Compose test manifest


    implementation("io.coil-kt:coil-compose:2.4.0")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")



    implementation(platform(libs.firebase.bom))

    // Firebase libraries (versions controlled by the BoM)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.storage.ktx)

    // Smooth image Navigations
   
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.33.2-alpha")


}



//dependencies {
//    // BoMs
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(platform(libs.firebase.bom))
//
//    // Compose
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.activity.compose)
//    implementation(libs.androidx.ui)
//    implementation(libs.androidx.ui.graphics)
//    implementation(libs.androidx.ui.tooling.preview)
//    implementation(libs.androidx.material3)
//    implementation(libs.androidx.navigation.compose)
//    implementation(libs.androidx.compose.material.icons.extended)
//
//    // Firebase (no explicit versions here, BoM controls them)
//    implementation(libs.firebase.analytics)
//    implementation(libs.firebase.storage.ktx)
//    implementation("com.google.firebase:firebase-auth-ktx")
//    implementation("com.google.firebase:firebase-firestore-ktx")
//
//    // Other
//    implementation("io.coil-kt:coil-compose:2.4.0")
//    implementation("androidx.media3:media3-exoplayer:1.4.1")
//    implementation("androidx.media3:media3-ui:1.4.1")
//
//    // Testing
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.ui.test.junit4)
//
//    // Debug
//    debugImplementation(libs.androidx.ui.tooling)
//    debugImplementation(libs.androidx.ui.test.manifest)
//}
