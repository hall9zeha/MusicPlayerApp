import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt.plugin)
    alias(libs.plugins.parcelize)

}

android {
    namespace = "com.barryzeha.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 24

    }
   /* packaging{
        resources {
            excludes += "META-INF/gradle/incremental.annotation.processors"

        }
    }*/
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

}

dependencies{

    implementation(libs.media3.explorer)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)
    // Room
    ksp(libs.room.compiler)

    // Dagger hilt
    ksp(libs.dagger.hilt.compiler)
    implementation(libs.dagger.hilt)
    // Modules
    implementation(project(":core"))

    // Test module
    testImplementation(projects.core.testing)
}