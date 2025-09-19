plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.devtools.ksp)
}

android {
    namespace = "com.youtubeapis"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.youtubeapis"
        minSdk = 24
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
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation ("androidx.media3:media3-cast:1.8.0")
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.media3.exoplayer.ima)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // HLS support (required for .m3u8 playback)
    implementation ("androidx.media3:media3-exoplayer-hls:1.8.0")

    implementation ("androidx.media3:media3-session:1.8.0")


    // Optional: DASH support (if you need .mpd playback)
    implementation ("androidx.media3:media3-exoplayer-dash:1.8.0")

    // Optional: SmoothStreaming
    implementation ("androidx.media3:media3-exoplayer-smoothstreaming:1.8.0")

    // RTSP (IP cameras etc.)
    implementation ("androidx.media3:media3-exoplayer-rtsp:1.8.0")


    // Kotlin Extensions and Coroutines support for Room
    implementation ("androidx.room:room-ktx:2.6.1")

    // Retrofit for HTTP requests
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")

    // Gson converter for parsing JSON
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation ("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

    implementation ("com.google.android.gms:play-services-auth:21.0.0")
   // implementation ("com.github.mmoamenn:LuckyWheel_Android:0.3.0")

    // Room Database
    implementation(libs.room.runtime) // Room database runtime
    ksp(libs.room.compiler) // Room annotation processor
    implementation(libs.androidx.room.ktx) // Kotlin extensions for Room database
    testImplementation(libs.androidx.room.testing) // Testing utilities for Room
    implementation ("com.github.bumptech.glide:glide:5.0.3")
    implementation ("com.github.chrisbanes:PhotoView:2.3.0")




    // Google Cast SDK
    implementation("com.google.android.gms:play-services-cast-framework:21.4.0")




    // implementation ("com.yuyakaido.android:card-stack-view:2.3.4")

}