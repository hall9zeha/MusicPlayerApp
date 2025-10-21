plugins {
    id("java-library")
    alias(libs.plugins.jetbrainsKotlinJvm)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}
dependencies{
    // Tests third party libraries
    api("io.mockk:mockk:1.14.6")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    api("junit:junit:4.13.2")
    api("com.google.truth:truth:1.4.4")
}
