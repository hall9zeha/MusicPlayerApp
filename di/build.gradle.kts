plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
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

    // Modules
    implementation(project(":core"))
    implementation(project(":data"))
}
