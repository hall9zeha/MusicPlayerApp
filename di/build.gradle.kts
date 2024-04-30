plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.dagger.hilt.plugin)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.barryzeha.di"
    compileSdk = 34
    defaultConfig {
        minSdk = 24

    }
    packaging{
        resources {
           excludes += "META-INF/gradle/incremental.annotation.processors"

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
dependencies{
    // Room
    ksp(libs.room.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    // Dagger hilt
    ksp(libs.dagger.hilt.compiler)

    implementation(libs.dagger.hilt)
    // Modules
    implementation(project(":core"))
    implementation(project(":data"))
}
