plugins {
    id("nav.common-root")
    alias(libs.plugins.versions)
    alias(libs.plugins.spotless)
}

repositories {
    mavenCentral()
}

spotless {
    java {
        target("*/src/*/java/**/*.java")
        palantirJavaFormat()
        forbidWildcardImports()
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlin {
        target("build-logic/src/**/*.kt")
        ktlint().setEditorConfigPath(file("build-logic/.editorconfig").path)
    }

    kotlinGradle {
        target(
            "*.gradle.kts",
            "*/build.gradle.kts",
            "build-logic/settings.gradle.kts",
            "scripts/publish-beta/*.gradle.kts",
        )
        ktlint().setEditorConfigPath(file("build-logic/.editorconfig").path)
    }
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = "9.9.1"
    distributionType = Wrapper.DistributionType.BIN
    distributionSha256Sum = "d8a18a7a841dd5df825d35a685fad6f2d7f12ac0c280e57b67c27b4a520ba228"
}
