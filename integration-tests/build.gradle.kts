@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(17)

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            testLogging {
                showStandardStreams = true
                events("failed")
            }
        }
    }

    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = "15s"
                }
            }
        }
    }

    wasmJs {
        nodejs()
    }

    macosArm64()

    sourceSets {
        commonTest {
            dependencies {
                implementation(project(":mokksy"))
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(project.dependencies.platform(libs.ktor.bom))
                implementation(libs.ktor.client.core)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.assertj.core)
                runtimeOnly(libs.slf4j.simple)
                runtimeOnly(libs.ktor.client.apache5)
            }
        }

        wasmJsTest {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }

        jsTest {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }

        nativeTest {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}
