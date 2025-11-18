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
  kapt(libs.micronaut.serde.processor)

  implementation(libs.picocli)
  implementation(libs.micronaut.kotlin.extension.functions)
  implementation(libs.micronaut.kotlin.runtime)
  implementation(libs.micronaut.picocli)
  implementation(libs.kotlin.reflect)
  implementation(libs.kotlin.stdlib.jdk8)
  implementation(libs.arrow.core)

  runtimeOnly(libs.logback.classic)

  testImplementation(libs.junit.platform.suite)
  testImplementation(libs.strikt.core)
  testImplementation(libs.strikt.arrow)
}

application {
    mainClass = "com.leeturner.cgol.GameOfLifeCommand"
}

java {
    sourceCompatibility = JavaVersion.toVersion("21")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
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

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    jdkVersion = "21"
}


