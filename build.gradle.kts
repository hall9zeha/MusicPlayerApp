// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {  }
  dependencies {
     classpath(libs.androidx.navigation.safeargs.plugin)
  }
}
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsKotlinJvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kapt) apply false
    alias(libs.plugins.dagger.hilt.plugin) apply false
    alias(libs.plugins.parcelize) apply false
    alias(libs.plugins.safeargs.plugin) apply false

}