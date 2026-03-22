import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `maven-publish`
    id("org.gradle.base")
    id("com.gradleup.shadow") // https://gradleup.com/shadow
}

afterEvaluate {
    // Remove test-related task dependencies added by atomicfu plugin
    tasks.named<ShadowJar>("shadowJar") {
        // Clear existing task dependencies
        setDependsOn(listOf(tasks.named("jvmJar")))

        // Use only main classes, not test classes
        configurations = listOf(project.configurations.getByName("jvmRuntimeClasspath"))

        // Set input from jvmJar only
        from(zipTree(tasks.named<Jar>("jvmJar").flatMap { it.archiveFile }))

        minimize {
            exclude(dependency("kotlin:.*:.*"))
            exclude(dependency("org.jetbrains.kotlin:.*:.*"))
            exclude(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-.*:.*"))
        }

        relocate("io.ktor", "dev.mokksy.relocated.io.ktor")
        relocate("kotlinx", "dev.mokksy.relocated.kotlinx")
    }
}

tasks.assemble {
    dependsOn(tasks.named("shadowJar"))
}
