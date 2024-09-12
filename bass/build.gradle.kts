plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)

}

android {
    namespace = "com.un4seen.bass"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    sourceSets{
        getByName("main"){
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}
dependencies{
    implementation(fileTree(mapOf("dir" to "libs","include" to listOf("*.jar"))))
}
