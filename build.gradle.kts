import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.dinhhuy258.vintellij"
version = "1.0.2"

buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath(kotlin("gradle-plugin", "1.4.10"))
    }
}

plugins {
    id("org.jetbrains.intellij") version "0.4.21"
    kotlin("jvm") version "1.4.10"
    id("org.jlleitschuh.gradle.ktlint") version "9.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    compile("org.msgpack", "msgpack-core", "0.8.16")
    compile("org.msgpack", "jackson-dataformat-msgpack", "0.8.16")
    compile("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.9.8")
    compile("org.scala-sbt.ipcsocket", "ipcsocket", "1.0.0")
    compile("org.eclipse.lsp4j", "org.eclipse.lsp4j", "0.9.0")
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "IU-202.6397.94"
    updateSinceUntilBuild = false

    // See https://plugins.jetbrains.com/plugin/6954-kotlin/versions/stable
    setPlugins("java", "org.jetbrains.kotlin:1.4.10-release-IJ2020.2-1")
}
