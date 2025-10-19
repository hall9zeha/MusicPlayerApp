plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt.plugin)
}

android {
    namespace = "com.barryzeha.di"
    compileSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
}
dependencies{
    // The compiler with ksp or kapt always is necessary in each module when use a library that require
    // can't make globally as "api" modifier implementation

    // Room
    ksp(libs.room.compiler)

    // Dagger hilt
    ksp(libs.dagger.hilt.compiler)
    implementation(libs.dagger.hilt)

    // Modules
    implementation(project(":core"))
    implementation(project(":data"))
}
