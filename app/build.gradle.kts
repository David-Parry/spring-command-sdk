plugins {
    id("java")
    id("application")
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.unbroken-dome.test-sets") version "4.1.0"
}

group = rootProject.group
version = rootProject.version

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Depend on internal-core
    implementation(project(":internal-core"))
    
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-integration")
    
    // ActiveMQ for messaging implementation
    implementation("org.springframework.boot:spring-boot-starter-activemq")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
}

application {
    mainClass.set("ai.qodo.command.app.QodoCommandApplication")
}

// Generate build-info.properties for version information in banner
springBoot {
    buildInfo {
        properties {
            additional.put("internal-core.version", project(":internal-core").version.toString())
        }
    }
}

// Ensure build-info is generated before resources are processed (for IDE support)
tasks.named<ProcessResources>("processResources") {
    dependsOn("bootBuildInfo")
    
    // Enable resource filtering to replace @...@ placeholders only
    filesMatching("application.yml") {
        filter<org.apache.tools.ant.filters.ReplaceTokens>(
            "tokens" to mapOf(
                "project.version" to version.toString(),
                "internal-core.version" to project(":internal-core").version.toString()
            )
        )
    }
}

// Also ensure it runs before compileJava for IDE builds
tasks.named("compileJava") {
    dependsOn("bootBuildInfo")
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName.set("command-sdk.jar")
    
    // Include internal-core resources in BOOT-INF/classes so Spring Boot can find them
    // The app module's application.yml will be processed first, then internal-core's
    // Spring Boot will merge them with app taking precedence
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    
    from(project(":internal-core").sourceSets.main.get().resources) {
        into("BOOT-INF/classes")
    }
    
    // Ensure build-info.properties is included in the bootJar in the correct location
    dependsOn("bootBuildInfo")
    from(layout.buildDirectory.file("resources/main/META-INF/build-info.properties")) {
        into("BOOT-INF/classes/META-INF")
    }
}

testSets {
    create("integrationTest")
}

tasks.named<Test>("integrationTest") {
    useJUnitPlatform()
}
