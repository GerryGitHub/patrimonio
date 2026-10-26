import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.1.2"
}

group = "com.patrimonio"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

jacoco {
    // 0.8.16 es la primera versión que soporta oficialmente Java 25 y 26.
    toolVersion = "0.8.16"
}

val integrationTestSourceSet = sourceSets.create("integrationTest") {
    java.srcDir("src/integrationTest/java")
    resources.srcDir("src/integrationTest/resources")
    // El esquema real se publica en el classpath de integración para que
    // Testcontainers lo aplique con withInitScript("esquema.sql").
    resources.srcDir("db")
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[integrationTestSourceSet.implementationConfigurationName].extendsFrom(
    configurations.testImplementation.get()
)
configurations[integrationTestSourceSet.runtimeOnlyConfigurationName].extendsFrom(
    configurations.testRuntimeOnly.get()
)

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.2"))
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.2"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    // ArchUnit se usa solo por su API core, sin el motor JUnit 6, para no
    // arrastrar una versión de JUnit Platform distinta a la del BOM de Boot.
    testImplementation("com.tngtech.archunit:archunit:1.5.1")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.test {
    description = "Runs unit tests. It does not require Docker."
}

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests against an isolated PostgreSQL 16 container."
    group = "verification"
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath
    shouldRunAfter(tasks.test)
}

fun jacocoExecutionData(task: TaskProvider<Test>) =
    task.map { it.extensions.getByType(JacocoTaskExtension::class.java).destinationFile }

val jacocoIntegrationTestReport = tasks.register<JacocoReport>("jacocoIntegrationTestReport") {
    description = "Genera el informe de cobertura de las pruebas de integración."
    group = "verification"
    dependsOn(integrationTest)
    executionData.setFrom(jacocoExecutionData(integrationTest))
    sourceDirectories.setFrom(sourceSets.main.get().allJava.srcDirs)
    classDirectories.setFrom(sourceSets.main.get().output.classesDirs)
    reports {
        xml.required.set(true)
        html.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/integrationTest/jacocoIntegrationTest.xml")
        )
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/integrationTest/html"))
    }
}

val jacocoMergedReport = tasks.register<JacocoReport>("jacocoMergedReport") {
    description = "Genera el informe de cobertura combinado de unitarias e integración."
    group = "verification"
    dependsOn(tasks.test, integrationTest)
    executionData.setFrom(
        jacocoExecutionData(tasks.test),
        jacocoExecutionData(integrationTest)
    )
    sourceDirectories.setFrom(sourceSets.main.get().allJava.srcDirs)
    classDirectories.setFrom(sourceSets.main.get().output.classesDirs)
    reports {
        xml.required.set(true)
        html.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/merged/jacocoMerged.xml")
        )
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/merged/html"))
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named("check") {
    dependsOn(integrationTest)
}
