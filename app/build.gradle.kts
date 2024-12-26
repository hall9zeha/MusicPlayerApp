plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    // En gradle kts no requiere  agregar apply false a los plugins
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt.plugin)
    alias(libs.plugins.parcelize)
}

android {
    namespace = "com.barryzeha.ktmusicplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.barryzeha.ktmusicplayer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    flavorDimensions.add("version")
    productFlavors {
        create("exoplayer"){
            dimension="version"
            applicationId="com.barryzeha.ktmusicplayer.exoplayer"
            manifestPlaceholders["appLabel"]="KTMusic Exo"
        }
        create("bass"){
            dimension="version"

            applicationId="com.barryzeha.ktmusicplayer.bass"
            manifestPlaceholders["appLabel"]="KTMusic Bass"
        }

    }
    lint {
        baseline = file("lint-baseline.xml")
    }
  /*  packaging{
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
   buildFeatures{
        viewBinding= true
        buildConfig=true
    }
}
val bassImplementation by configurations
dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // JaudioTagger
    implementation(libs.jaudiotagger)
    // Glide
    implementation(libs.glide)
    ksp(libs.glide.compiler)
    //media3
    implementation(libs.media3.explorer)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)
    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    // Dagger hilt
    ksp(libs.dagger.hilt.compiler)
    implementation(libs.dagger.hilt)
    // Gson
    implementation(libs.gson)
    // Fast scroll
    implementation(libs.fast.scroll)
    // Disc cover view
    implementation(libs.disc.cover.view)
    // Splash screen
    implementation(libs.core.splashscreen)
    // Modules
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":di"))
    implementation(project(":features:mfilepicker"))
    implementation(project(":features:audioeffects"))
    // Only will charge this module when using bass flavor implementation
    bassImplementation(project(":bass"))


}