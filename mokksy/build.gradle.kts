@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.serialization)
    `kotlin-multiplatform-convention`
    `dokka-convention`
    `publish-convention`
    `netty-convention`
    `shadow-convention`
    alias(libs.plugins.kotlinx.atomicfu) apply true
    alias(libs.plugins.kover) apply true
    alias(libs.plugins.knit)
}

dokka {
    dokkaSourceSets.configureEach {
        // includes.from("README.md")
    }

    moduleName.set("Mokksy")

    pluginsConfiguration.html {
        footerMessage = "Copyright © 2025-2026 Konstantin Pavlov"
    }
}

kotlin {

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotest.assertions.core)
                api(libs.kotest.assertions.json)
                api(libs.ktor.server.content.negotiation)
                api(libs.ktor.server.core)
                api(project.dependencies.platform(libs.ktor.bom))
                implementation(libs.kotlinx.atomicfu)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.server.double.receive)
                implementation(libs.ktor.server.sse)
                implementation(libs.kotlinLogging)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.assertk)
                implementation(libs.kotlinLogging)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.server.test.host)
            }
        }

        webMain {
            dependencies {
                implementation(libs.ktor.server.cio)
            }
        }

        jvmMain {
            dependencies {
                implementation(project.dependencies.platform(libs.netty.bom))
                implementation(libs.jansi)
                implementation(libs.ktor.server.call.logging)
                implementation(libs.ktor.server.netty)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.datafaker)
                implementation(libs.junit.jupiter.params)
                implementation(libs.ktor.client.java)
                implementation(libs.ktor.serialization.jackson)
                implementation(libs.lincheck)
                implementation(libs.mockk)
                implementation(libs.mockk.dsl)
                runtimeOnly(libs.slf4j.simple)
            }
        }

        nativeMain {
            dependencies {
                implementation(libs.ktor.server.cio)
            }
        }

        nativeTest {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }

    sourceSets {
        jvmTest {
            kotlin.srcDir("build/generated/knit/test/kotlin")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            artifactId = "${project.name}-standalone"
            artifact(tasks.named("shadowJar")) {
                classifier = ""
                extension = "jar"
            }
            artifact(tasks["jvmSourcesJar"]) {
                classifier = "sources"
            }
        }
    }
}

knit {
    rootDir = project.rootDir
    files =
        fileTree(project.rootDir) {
            include("README.md")
            exclude("**/build/**")
        }
    siteRoot = "https://mokksy.dev/"
}
// Generated knit sources must exist before test compilation.
tasks.named("jvmTestClasses").configure {
    dependsOn(tasks.named("knit"))
}

// Decouple knit tasks from the standard build lifecycle.
// Run on demand: ./gradlew :docs:knit  or  ./gradlew :docs:knitCheck
// afterEvaluate is required here because the knit plugin wires knitCheck -> check during its own afterEvaluate.
afterEvaluate {
    tasks.named("check").configure {
        setDependsOn(
            dependsOn.filterNot { dep ->
                (dep is TaskProvider<*> && dep.name == "knitCheck") ||
                    (dep is Task && dep.name == "knitCheck") ||
                    (dep is String && dep == "knitCheck")
            },
        )
    }
}
