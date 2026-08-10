group = "x.core"
version = "0.0.1"

plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.bundles.junit)
    testImplementation(libs.mockito.core)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    withType<Wrapper> {
        gradleVersion = libs.versions.gradle.get()
    }
}
