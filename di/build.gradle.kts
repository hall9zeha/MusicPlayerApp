plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt.plugin)
}

android {
    namespace = "com.barryzeha.di"
    compileSdk = 34
    defaultConfig {
        minSdk = 24

    }
   /* packaging{
        resources {
           excludes += "META-INF/gradle/incremental.annotation.processors"

        }
    }*/
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
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
