import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "9.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("xyz.jpenilla.run-paper") version "2.1.0"
}

group = "com.example"
version = "1.0.0"

val runServerWorldNames = listOf("world", "world_nether", "world_the_end")
val runServerWorldSnapshot = layout.projectDirectory.dir("run/_world_reset_snapshot")

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    testImplementation(kotlin("test-junit5"))
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    assemble {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    register("captureRunServerWorldSnapshot") {
        group = "run paper"
        description = "Captures the current run server worlds as the reset baseline."

        doLast {
            val snapshotDir = runServerWorldSnapshot.asFile
            delete(snapshotDir)

            runServerWorldNames.forEach { worldName ->
                val worldDir = layout.projectDirectory.dir("run/$worldName").asFile
                if (worldDir.exists()) {
                    copy {
                        from(worldDir)
                        into(snapshotDir.resolve(worldName))
                    }
                }
            }
        }
    }

    register("resetRunServerWorld") {
        group = "run paper"
        description = "Restores the run server worlds from the captured baseline."

        doLast {
            val snapshotDir = runServerWorldSnapshot.asFile
            if (!snapshotDir.exists()) {
                throw GradleException(
                    "Missing run server world snapshot. Run './gradlew captureRunServerWorldSnapshot' first."
                )
            }

            runServerWorldNames.forEach { worldName ->
                delete(layout.projectDirectory.dir("run/$worldName").asFile)
                copy {
                    from(snapshotDir.resolve(worldName))
                    into(layout.projectDirectory.dir("run/$worldName"))
                }
            }
        }
    }

    runServer {
        dependsOn("resetRunServerWorld")
        minecraftVersion("1.21.11")
    }

    test {
        useJUnitPlatform()
    }

    named<Jar>("jar") {
        archiveClassifier.set("plain")
    }

    withType<ShadowJar> {
        archiveClassifier.set("")
    }
}

kotlin {
    jvmToolchain(21)
}
