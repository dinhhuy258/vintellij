import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.dinhhuy258.vintellij"
version = "1.0"

buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath(kotlin("gradle-plugin", "1.3.21"))
    }
}

plugins {
    id("org.jetbrains.intellij") version "0.4.21"
    id("org.jetbrains.kotlin.jvm") version "1.3.61"
    id("org.jlleitschuh.gradle.ktlint") version "9.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
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
    // See https://plugins.jetbrains.com/plugin/6954-kotlin/versions/stable
    setPlugins("java", "org.jetbrains.kotlin:1.4.0-release-IJ2020.2-1")
    version = "2020.2"
}
