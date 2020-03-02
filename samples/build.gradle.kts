import org.jetbrains.kotlin.gradle.plugin.*

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    sourceSets {
        val jvmMain: KotlinSourceSet by getting {
            kotlin.srcDir("src")

            dependencies {
                api(project(":kotlinx-io"))
                api(project(":kotlinx-io-playground"))
            }
        }
    }
}