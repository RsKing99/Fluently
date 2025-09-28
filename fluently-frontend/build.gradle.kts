import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask
import dev.karmakrafts.conventions.configureJava
import org.gradle.jvm.tasks.Jar as JvmJar
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.antlrKotlin)
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
            kotlin.srcDir(layout.buildDirectory.dir("generatedAntlr"))
            dependencies {
                api(libs.antlrKotlin.runtime)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.annotations)
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

tasks {
    val generateKotlinGrammarSource by register<AntlrKotlinTask>("generateKotlinGrammarSource") {
        group = "antlr"
        dependsOn("cleanGenerateKotlinGrammarSource")
        source = fileTree(layout.projectDirectory.dir("src/main/antlr")) {
            include("*.g4")
        }
        packageName = "${project.group}.frontend"
        arguments = listOf("-visitor")
        outputDirectory = layout.buildDirectory.dir("generatedAntlr/${packageName!!.replace('.', '/')}").get().asFile
    }
    withType<KotlinCompilationTask<*>> {
        dependsOn(generateKotlinGrammarSource)
    }
    withType<JvmJar> {
        dependsOn(generateKotlinGrammarSource)
    }
    named("prepareKotlinIdeaImport") {
        dependsOn(generateKotlinGrammarSource)
    }
}