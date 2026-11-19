pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "common-java-modules"

include(
    "audit-log", "auth", "client", "cxf", "feature-toggle", "json",
    "kafka", "log", "job", "metrics", "rest", "sts", "health", "test",
    "types", "util", "yaml", "nais", "token-client",
)
