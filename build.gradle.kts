plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kapt)
  alias(libs.plugins.kotlin.allopen)
  alias(libs.plugins.micronaut.application)
  alias(libs.plugins.shadow)
  alias(libs.plugins.detekt)
  alias(libs.plugins.kotlinter)
}

version = "0.1"
group = "com.leeturner.cgol"

repositories {
    mavenCentral()
}

dependencies {
  kapt(libs.picocli.codegen)

  implementation(libs.picocli)
  implementation(libs.micronaut.kotlin.runtime)
  implementation(libs.micronaut.picocli)
  implementation(libs.arrow.core)
  implementation(libs.korge)

  runtimeOnly(libs.logback.classic)

  testImplementation(libs.strikt.core)
  testImplementation(libs.strikt.arrow)
}

application {
    mainClass = "com.leeturner.cgol.GameOfLifeCommand"
    // KorGE's AWT/OpenGL window reflects into JDK internals and loads native code.
    // Same package list as KorGE's jvmAddOpensList(); the platform packages only exist on their own OS.
    val os = System.getProperty("os.name").lowercase()
    val platformPackages =
        when {
            "mac" in os -> listOf("sun.lwawt", "sun.lwawt.macosx", "com.apple.eawt", "com.apple.eawt.event")
            "linux" in os -> listOf("sun.awt.X11")
            else -> emptyList()
        }
    applicationDefaultJvmArgs =
        (listOf("sun.java2d.opengl", "java.awt", "sun.awt") + platformPackages)
            .map { "--add-opens=java.desktop/$it=ALL-UNNAMED" } + "--enable-native-access=ALL-UNNAMED"
}

java {
    sourceCompatibility = JavaVersion.toVersion("25")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

detekt {
  toolVersion = libs.versions.detekt.get()
  config.setFrom(file("config/detekt/detekt.yml"))
  buildUponDefaultConfig = true
}

micronaut {
    version(libs.versions.micronaut.version.get())
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.leeturner.cgol.*")
    }
}

tasks.named<io.micronaut.gradle.docker.MicronautDockerfile>("dockerfile") {
  baseImage = "eclipse-temurin:25-jre"
}

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    jdkVersion = "25"
}


