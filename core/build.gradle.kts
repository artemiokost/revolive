group = "revolive.core"
version = "0.0.1"

plugins {
    alias(libs.plugins.kotlin) apply true
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.kotlin)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.mockito)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
    jvmToolchain(21)
}

tasks {
    test {
        useJUnitPlatform()
    }

    withType<Wrapper> {
        gradleVersion = libs.versions.gradle.get()
    }
}