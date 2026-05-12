import org.jetbrains.kotlin.gradle.dsl.JvmTarget

group = "com.bhikadia.receive_intent"
version "1.0-SNAPSHOT"

plugins {
    id("com.android.library")
}

repositories {
        google()
        mavenCentral()
    }

android {
    namespace 'com.bhikadia.receive_intent'
    compileSdk = 37

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
    }
    defaultConfig {
        minSdk = 24
        targetSdk = 37
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.10.0")
}