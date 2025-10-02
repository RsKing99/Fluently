/*
 * Copyright 2025 Karma Krafts & associates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask
import dev.karmakrafts.conventions.configureJava
import dev.karmakrafts.conventions.setProjectInfo
import org.gradle.jvm.tasks.Jar as JvmJar
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.time.ZonedDateTime

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.antlrKotlin)
    alias(libs.plugins.dokka)
    signing
    `maven-publish`
}

configureJava(libs.versions.java)

@OptIn(ExperimentalWasmDsl::class) //
kotlin {
    withSourcesJar()
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

dokka {
    moduleName = project.name
    pluginsConfiguration {
        html {
            footerMessage = "(c) ${ZonedDateTime.now().year} Karma Krafts & associates"
        }
    }
}

val dokkaJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaGeneratePublicationHtml)
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

publishing {
    publications.withType<MavenPublication> {
        artifact(dokkaJar)
    }
    setProjectInfo("Fluently Frontend", "Lexer and parser frontend for the Fluently localization system")
}