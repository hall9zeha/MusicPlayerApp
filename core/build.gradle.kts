import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt.plugin)
    alias(libs.plugins.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.barryzeha.core"
    compileSdk = 37
    defaultConfig {
        minSdk = 24

    }
/*
    packaging{
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/gradle/incremental.annotation.processors"

        }
    }
*/
*/


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{LICENSE.md,LICENSE-notice.md}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.transition)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // JaudioTagger
    implementation(libs.jaudiotagger)

    // Glide
    implementation(libs.glide)
    ksp(libs.glide.compiler)
    // Gson
    implementation(libs.gson)
    //
    // Room
    ksp(libs.room.compiler)
    api(libs.room.runtime)
    api(libs.room.ktx)

    // Dagger hilt
    ksp(libs.dagger.hilt.compiler)
    implementation(libs.dagger.hilt)
    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    // Splash screen
    implementation(libs.core.splashscreen)
    // Json
    implementation(libs.serialization.json)

    // Testing module
    testImplementation(projects.core.testing)
    androidTestImplementation(projects.core.testing)
}