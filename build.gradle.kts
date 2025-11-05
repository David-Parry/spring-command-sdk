plugins {
    id("java")
    id("org.springframework.boot") version "3.5.6" apply false
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ai.qodo.command"
version = project.findProperty("version") as String? ?: "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

// Root project - no dependencies or application configuration
// All functionality is in submodules: internal-core and app
