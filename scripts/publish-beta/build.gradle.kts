import org.gradle.api.publish.maven.tasks.GenerateMavenPom

plugins {
    `maven-publish`
}

val revision = providers.gradleProperty("revision").get()
val stagedGroup = file(providers.gradleProperty("stagingRepository").get()).resolve("no/nav/common")
val modules = stagedGroup.listFiles().orEmpty().filter { it.isDirectory }.sortedBy { it.name }
check(modules.isNotEmpty()) { "Fant ingen moduler i $stagedGroup" }

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri(
                providers.gradleProperty("publicationRepository")
                    .getOrElse("https://maven.pkg.github.com/navikt/common-java-modules"),
            )
            if (url.scheme == "https") {
                credentials {
                    username = providers.environmentVariable("GITHUB_ACTOR").orNull
                    password = providers.environmentVariable("GITHUB_TOKEN").orNull
                }
            }
        }
    }
}

modules.forEach { module ->
    val prefix = module.resolve("$revision/${module.name}-$revision")
    val pomFile = file("$prefix.pom")
    check(pomFile.isFile) { "Mangler ferdigbygd POM: $pomFile" }

    publishing.publications.create<MavenPublication>(module.name) {
        groupId = "no.nav.common"
        artifactId = module.name
        version = revision
        listOf("", "sources", "tests", "test-sources").forEach { suffix ->
            val jarFile = file("$prefix${if (suffix.isEmpty()) "" else "-$suffix"}.jar")
            check(jarFile.isFile) { "Mangler ferdigbygd JAR: $jarFile" }
            artifact(jarFile) {
                classifier = suffix.ifEmpty { null }
            }
        }
    }

    // Bruk den testede POM-filen uendret i stedet for å generere en ny uten avhengighetene.
    tasks.named<GenerateMavenPom>("generatePomFileFor${module.name.replaceFirstChar { it.uppercaseChar() }}Publication") {
        destination = pomFile
        enabled = false
    }
}
