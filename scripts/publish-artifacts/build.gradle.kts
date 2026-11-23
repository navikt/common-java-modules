import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.GenerateMavenPom

val revision = providers.gradleProperty("revision").get()
val stagedGroup = file(providers.gradleProperty("stagingRepository").get()).resolve("no/nav/common")

subprojects {
    pluginManager.apply("maven-publish")
    val moduleName = name
    val prefix = stagedGroup.resolve("$moduleName/$revision/$moduleName-$revision")
    val pomFile = file("$prefix.pom")
    check(pomFile.isFile) { "Mangler ferdigbygd POM: $pomFile" }

    extensions.configure<PublishingExtension> {
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

        publications.create<MavenPublication>("mavenJava") {
            groupId = "no.nav.common"
            artifactId = moduleName
            version = revision
            listOf("", "sources", "tests", "test-sources").forEach { suffix ->
                val jarFile = file("$prefix${if (suffix.isEmpty()) "" else "-$suffix"}.jar")
                check(jarFile.isFile) { "Mangler ferdigbygd JAR: $jarFile" }
                artifact(jarFile) {
                    classifier = suffix.ifEmpty { null }
                }
            }
        }
    }

    // Bruk den testede POM-filen uendret i stedet for å generere en ny uten avhengighetene.
    tasks.named<GenerateMavenPom>("generatePomFileForMavenJavaPublication") {
        destination = pomFile
        enabled = false
    }
}
