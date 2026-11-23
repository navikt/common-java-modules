import org.gradle.api.tasks.testing.Test

plugins {
    `kotlin-dsl`
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.version.get().toInt())
}

dependencies {
    implementation(localGroovy())
    implementation("io.github.ben-manes.versions:io.github.ben-manes.versions.gradle.plugin:${libs.versions.versions.plugin.get()}")
    testImplementation("org.junit.jupiter:junit-jupiter:${libs.versions.junit.jupiter.get()}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${libs.versions.junit.jupiter.get()}")
}

tasks.named<Test>("test") {
    inputs.file("../gradle/libs.versions.toml").withPathSensitivity(PathSensitivity.RELATIVE)
    // POM-testene kopierer modulenes byggskript, så endringer i dem må utløse ny testkjøring.
    inputs.files(fileTree("..") { include("*/build.gradle.kts") }).withPathSensitivity(PathSensitivity.RELATIVE)
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("commonJava") {
            id = "nav.common-java"
            implementationClass = "no.nav.common.build.CommonJavaPlugin"
        }
        register("commonRoot") {
            id = "nav.common-root"
            implementationClass = "no.nav.common.build.CommonRootPlugin"
        }
        register("commonLibrary") {
            id = "nav.common-library"
            implementationClass = "no.nav.common.build.CommonLibraryPlugin"
        }
        register("commonPublishing") {
            id = "nav.common-publishing"
            implementationClass = "no.nav.common.build.CommonPublishingPlugin"
        }
    }
}
