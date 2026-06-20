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

    runServer {
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
