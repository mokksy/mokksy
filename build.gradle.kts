import dev.detekt.gradle.extensions.FailOnSeverity

plugins {
    base
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.nexusPublish) // https://github.com/gradle-nexus/publish-plugin
    alias(libs.plugins.openrewrite)
    kotlin("plugin.serialization") version libs.versions.kotlin apply false
    `dokka-convention`
    signing
}

allprojects {
    repositories {
        mavenCentral()
    }
}

// Common configuration for subprojects
subprojects {
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "dev.detekt")

    detekt {
        config.from(rootProject.file("detekt.yml"))
        buildUponDefaultConfig = true
        parallel = true
        debug = false
        failOnSeverity = FailOnSeverity.Warning
    }
}

dependencies {
    kover(project(":mokksy"))
}

kover {
    reports {
        filters {
            includes {
                classes("dev.mokksy.mokksy.*")
            }
            excludes {
                classes("**.*StressTest*")
            }
        }

        total {
            xml
            html
        }

        verify {
            rule {
                bound {
                    minValue = 75
                }
            }
        }
    }
}

rewrite {
    activeRecipe(
//        "org.openrewrite.kotlin.format.AutoFormat",
        "org.openrewrite.gradle.MigrateToGradle8",
        "org.openrewrite.gradle.RemoveRedundantDependencyVersions",
        "org.openrewrite.kotlin.cleanup.RemoveLambdaArgumentParentheses",
        "org.openrewrite.kotlin.cleanup.UnnecessaryTypeParentheses",
    )
    isExportDatatables = true
}
