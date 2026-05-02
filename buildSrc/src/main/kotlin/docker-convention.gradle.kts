import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerRemoveImage

plugins {
    id("com.bmuschko.docker-remote-api")
}

val dockerImageRepository =
    project.findProperty("dockerImageName") as String? ?: "mokksy/server-jvm"

val dockerImageTag =
    project.findProperty("dockerImageTag") as String? ?: "snapshot"

val dockerImageName = "$dockerImageRepository:$dockerImageTag"

docker {
}

val dockerRuntime: Configuration by configurations.creating {
    isTransitive = true
}

val libs =
    extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")

dependencies {
    dockerRuntime(libs.findLibrary("slf4j-simple").get())
}

afterEvaluate {
    val dockerStagingDir = project.layout.buildDirectory.dir("docker/context")

    val prepareDockerContext by tasks.registering(Sync::class) {
        description = "Prepare files for building docker image"
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(tasks.named("jvmJar"))

        // JAR
        from(tasks.named<Jar>("jvmJar").flatMap { it.archiveFile })
        // Runtime dependencies
        from(configurations.named("jvmRuntimeClasspath")) { into("lib") }
        from(configurations.named("dockerRuntime")) { into("lib") }
        // Dockerfile
        from(project.projectDir) { include("Dockerfile") }

        into(dockerStagingDir)
        // Rename the versioned JAR to the fixed name expected by the Dockerfile
        rename("mokksy-jvm-.+\\.jar", "mokksy-jvm.jar")
    }

    tasks.register<DockerBuildImage>("dockerBuildImage") {
        description = "Builds the docker image"
        group = "docker"
        dependsOn(prepareDockerContext)

        inputDir.set(dockerStagingDir)
        dockerFile.set(dockerStagingDir.map { it.file("Dockerfile") })
        images.add(dockerImageName)
    }

    tasks.named("assemble") {
        finalizedBy(tasks.named("dockerBuildImage"))
    }

    tasks.register<DockerRemoveImage>("dockerRemoveImage") {
        description =
            "Removes the locally built Docker image. Run explicitly; not wired into 'clean'."
        group = "docker"
        targetImageId(dockerImageName)
        onError {
            // Image not present locally — nothing to remove, not an error.
            if (!message.orEmpty().contains("image not known")) throw this
        }
    }

    tasks.named("clean") {
        dependsOn(tasks.named("dockerRemoveImage"))
    }
}
