package no.nav.common.build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/** Setter opp Maven-publisering og kompatibilitetsmetadata for prosjektets artifakter. */
@Suppress("unused")
class CommonPublishingPlugin : Plugin<Project> {
    /** Registrerer Maven-publisering, POM-konfigurasjon og metadata i JAR-filer. */
    override fun apply(project: Project) {
        project.pluginManager.apply("maven-publish")

        project.tasks.withType<GenerateModuleMetadata>().configureEach {
            // Gradle-konsumenter skal også bruke POM-kontrakten, ikke en parallell metadatamodell.
            enabled = false
        }

        val publication = project.createCommonPublication()
        project.configureCommonPublicationPom(publication)

        project.configureGitHubPackagesRepository()
        project.providers.gradleProperty("stagingRepository").orNull?.let { directory ->
            project.extensions.configure<PublishingExtension> {
                repositories.maven {
                    name = "Staging"
                    url = project.uri(directory)
                }
            }
        }
        project.embedMavenPublicationMetadata()
    }
}
