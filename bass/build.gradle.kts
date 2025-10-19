plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)

}

android {
    namespace = "com.un4seen.bass"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        ndk {
            // Solo agregar las arquitecturas para las cuales irá destinada nuestra aplicación, reducirá el tamaño
            // de la misma, para pruebas mantenemos x86 y "x86_64"
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    sourceSets{
        getByName("main"){
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}
dependencies{
    implementation(fileTree(mapOf("dir" to "libs","include" to listOf("*.jar"))))
    implementation(fileTree(mapOf("dir" to "src/main/jniLibs","include" to listOf("*.so"))))

}
