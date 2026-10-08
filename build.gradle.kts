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

    val resetRunWorld by registering {
        group = "run paper"
        description = "Deletes the run worlds and forces an empty superflat world so every runServer starts clean."

        doLast {
            val runDir = layout.projectDirectory.dir("run").asFile
            listOf("world", "world_nether", "world_the_end").forEach { delete(runDir.resolve(it)) }

            // level-type only applies when a world is created, so it must be set before the fresh world generates
            val properties = runDir.resolve("server.properties")
            if (properties.isFile) {
                val forced = mapOf(
                    "level-type" to "minecraft\\:flat",
                    "generator-settings" to "{}",
                    "generate-structures" to "false",
                )
                val lines = properties.readLines().filterNot { line -> forced.keys.any { line.startsWith("$it=") } }
                properties.writeText((lines + forced.map { (key, value) -> "$key=$value" }).joinToString("\n", postfix = "\n"))
            }
        }
    }

    runServer {
        dependsOn(resetRunWorld)
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
