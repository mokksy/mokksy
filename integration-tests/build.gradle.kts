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
            javaParameters = true
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
                implementation(project.dependencies.platform(libs.ktor.bom))
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.auth)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.server.auth)
                implementation(libs.ktor.server.content.negotiation)
                implementation(libs.ktor.server.double.receive)
                implementation(libs.ktor.server.sse)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.assertj.core)
                implementation(libs.junit.jupiter.params)
                implementation(libs.ktor.serialization.jackson)
                implementation(libs.ktor.server.test.host)
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
