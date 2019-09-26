import org.gradle.api.internal.plugins.UnixStartScriptGenerator
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.math.max
import java.util.Properties


plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    kotlin("jvm") version "1.3.50"
    `maven-publish`
    application
    id("org.jetbrains.dokka") version "0.9.18"
    id("com.jfrog.bintray") version "1.7.3"
}

val versionProps = Properties().also {
    it.load(File("$projectDir/src/main/resources/version.properties").inputStream())
}
version = versionProps["version"] as String
group = "net.razorvine"
base.archivesBaseName = "ksim65"

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
    maven("https://jitpack.io")
}

dependencies {
    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.1.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.1.0")
}

tasks {
    named<Test>("test") {
        // Enable JUnit 5 (Gradle 4.6+).
        useJUnitPlatform()
        // Always run tests, even when nothing changed.
        dependsOn("cleanTest")
        // Show test results.
        testLogging.events("failed")

        // parallel tests.
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        maxParallelForks = max(1, Runtime.getRuntime().availableProcessors() / 2)
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    named<DokkaTask>("dokka") {
        outputFormat = "html"
        outputDirectory = "$buildDir/kdoc"
        skipEmptyPackages = true
    }
}

val c64emuScript by tasks.registering(CreateStartScripts::class) {
    outputDir = File(project.buildDir, "bin")
    applicationName = "c64emu"
    mainClassName = "razorvine.c64emu.C64MainKt"
    classpath = project.tasks["jar"].outputs.files + project.configurations.runtimeClasspath.get()
}

val ehbasicScript by tasks.registering(CreateStartScripts::class) {
    outputDir = File(project.buildDir, "bin")
    applicationName = "ehbasic"
    mainClassName = "razorvine.examplemachines.EhBasicMainKt"
    classpath = project.tasks["jar"].outputs.files + project.configurations.runtimeClasspath.get()
}

application {
    applicationName = "ksim65vm"
    mainClassName = "razorvine.examplemachines.MachineMainKt"
    applicationDistribution.into("bin") {
        from(c64emuScript, ehbasicScript)
        fileMode = 493
    }
}
