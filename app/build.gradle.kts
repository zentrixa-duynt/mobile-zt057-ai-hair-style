plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.legacy.kapt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.example.aihair"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.aihair"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        resValues = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
        }
    }

    flavorDimensions += "environment"

    productFlavors {
        create("dev") {
            dimension = "environment"
            // Tạm thời tắt dòng này để khớp với google-services.json hiện tại
            // applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"

            resValue("string", "app_name", "AI Hair (Dev)")

            // AdMob App ID Test
            manifestPlaceholders["ad_app_id"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "adAppId", "\"ca-app-pub-3940256099942544~3347511713\"")
            buildConfigField("Boolean", "buildDebug", "true")
            buildConfigField("Boolean", "isProduction", "false")
        }

        create("internal") {
            dimension = "environment"

            resValue("string", "app_name", "AI Hair (Internal)")

            // TODO: Thay bằng ID AdMob thật của bạn khi test nội bộ
            manifestPlaceholders["ad_app_id"] = "<THAY_ID_ADMOB_THAT_CUA_BAN_VAO_DAY>"
            buildConfigField("String", "adAppId", "\"<THAY_ID_ADMOB_THAT_CUA_BAN_VAO_DAY>\"")
            buildConfigField("Boolean", "buildDebug", "false")
            buildConfigField("Boolean", "isProduction", "false")
        }

        create("prod") {
            dimension = "environment"

            resValue("string", "app_name", "AI Hair")

            // TODO: Thay bằng ID AdMob thật của bạn khi release
            manifestPlaceholders["ad_app_id"] = "<THAY_ID_ADMOB_THAT_CUA_BAN_VAO_DAY>"
            buildConfigField("String", "adAppId", "\"<THAY_ID_ADMOB_THAT_CUA_BAN_VAO_DAY>\"")
            buildConfigField("Boolean", "buildDebug", "false")
            buildConfigField("Boolean", "isProduction", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val appName = "AIHair"
            output.outputFileName.set("${appName}.apk")
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation(libs.androidx.appcompat)
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Lifecycle & ViewModels
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Glide
    implementation(libs.glide)
    implementation(libs.glide.okhttp3.integration)
    kapt(libs.glide.compiler)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Retrofit & Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // Lottie
    implementation("com.airbnb.android:lottie:6.6.7")

    // Titan SDK
    implementation("com.github.zentrixa.titan:titan-android-sdk:1.0.0.beta08")

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.storage)
    implementation(libs.firebase.config)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)

    // Coroutines Play Services (for .await() on Firebase Tasks)
    implementation(libs.kotlinx.coroutines.play.services)

    // BlurView
    implementation("com.github.Dimezis:BlurView:version-3.2.0")
}