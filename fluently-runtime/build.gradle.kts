import dev.karmakrafts.conventions.configureJava
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    signing
    `maven-publish`
}

configureJava(libs.versions.java)

@OptIn(ExperimentalWasmDsl::class) //
kotlin {
    jvm()
    mingwX64()
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()
    androidTarget {
        publishLibraryVariants("release", "debug")
    }
    androidNativeX64()
    androidNativeArm64()
    androidNativeArm32()
    iosX64()
    iosArm64()
    js {
        browser()
    }
    wasmJs {
        browser()
    }
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.fluentlyFrontend)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.io.core)
            }
        }
    }
}

android {
    namespace = "$group.${rootProject.name}"
    compileSdk = libs.versions.androidCompileSDK.get().toInt()
    defaultConfig {
        minSdk = libs.versions.androidMinimalSDK.get().toInt()
    }
}