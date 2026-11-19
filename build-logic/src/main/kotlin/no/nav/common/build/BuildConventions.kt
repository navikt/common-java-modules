package no.nav.common.build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.the

/** Standardversjon uten `-Prevision`. */
const val DEFAULT_REVISION = "1-SNAPSHOT"

internal const val COMMON_GROUP = "no.nav.common"

/** Slår opp en versjon i versjonskatalogen, med punktum normalisert til bindestrek. */
internal fun Project.libraryVersion(alias: String): String = the<VersionCatalogsExtension>()
    .named("libs")
    .findVersion(alias.replace('.', '-'))
    .get()
    .requiredVersion

/** Setter felles gruppe, versjon og beskrivelse for prosjektet. */
internal fun Project.configureIdentity() {
    group = COMMON_GROUP
    version = providers.gradleProperty("revision").getOrElse(DEFAULT_REVISION)
    description = "Felles java moduler for produktområde arbeidsoppfølging (POAO)"
}

/** Publiserer rotprosjektets parent-POM (`mor-pom`) for Maven-konsumenter. */
internal fun Project.configureRootPomPublication() {
    pluginManager.apply("maven-publish")
    val moduleNames = subprojects.map { it.name }.sorted()
    val parentMetadata = createParentPomMetadata()

    val publishing = the<PublishingExtension>()
    publishing.repositories.maven("https://maven.pkg.github.com/navikt/common-java-modules") {
        name = "GitHubPackages"
        credentials {
            username = providers.environmentVariable("GITHUB_ACTOR").orNull
            password = providers.environmentVariable("GITHUB_TOKEN").orNull
        }
    }

    val publication = publishing.publications.create<MavenPublication>("morPom") {
        artifactId = "mor-pom"
    }

    publication.pom {
        name.set("Common Java Modules")
        description.set(requireNotNull(project.description))
        url.set("https://github.com/navikt/common-java-modules")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                organization.set(
                    "NAV (Arbeids- og velferdsdirektoratet) - The Norwegian Labour and Welfare Administration",
                )
                organizationUrl.set("https://www.nav.no")
            }
        }
    }

    publication.pom.withXml {
        val projectNode = asNode()
        val properties = projectNode.appendNode("properties")
        properties.appendNode("revision", DEFAULT_REVISION)
        properties.appendNode("java.version", parentMetadata.javaVersion)
        properties.appendNode("project.build.sourceEncoding", "UTF-8")
        properties.appendNode("sonar.organization", "navikt")
        properties.appendNode("sonar.host.url", "https://sonarcloud.io")
        parentMetadata.properties.forEach { (name, value) -> properties.appendNode(name, value) }

        val repository = projectNode.appendNode("distributionManagement").appendNode("repository")
        repository.appendNode("id", "github")
        repository.appendNode("url", "https://maven.pkg.github.com/navikt/common-java-modules")

        val dependencies = projectNode.appendNode("dependencyManagement").appendNode("dependencies")
        moduleNames.forEach { moduleName ->
            val dependency = dependencies.appendNode("dependency")
            dependency.appendNode("groupId", COMMON_GROUP)
            dependency.appendNode("artifactId", moduleName)
            dependency.appendNode("version", $$"${project.version}")
            if (moduleName == "test") dependency.appendNode("scope", "test")
            if (moduleName == "cxf") dependency.appendMavenExclusion("commons-logging", "commons-logging")

            if (moduleName in setOf("auth", "json", "metrics", "test")) {
                val testDependency = dependencies.appendNode("dependency")
                testDependency.appendNode("groupId", COMMON_GROUP)
                testDependency.appendNode("artifactId", moduleName)
                testDependency.appendNode("version", $$"${project.version}")
                testDependency.appendNode("type", "test-jar")
                testDependency.appendNode("scope", "test")
            }
        }

        parentMetadata.dependencies.forEach { managedDependency ->
            val dependency = dependencies.appendNode("dependency")
            dependency.appendNode("groupId", managedDependency.groupId)
            dependency.appendNode("artifactId", managedDependency.artifactId)
            dependency.appendNode("version", managedDependency.version)
            managedDependency.scope?.let { dependency.appendNode("scope", it) }
            if (managedDependency.optional) dependency.appendNode("optional", "true")
            if (managedDependency.excludeTransitives) dependency.appendMavenExclusion("*", "*")
        }

        projectNode.addMavenParentBuild()
    }
}

/** Konfigurerer rotprosjektets oppgaver og publisering. */
@Suppress("unused")
class CommonRootPlugin : Plugin<Project> {
    /** Registrerer rotprosjektets egne oppgaver uten å konfigurere modulene. */
    override fun apply(project: Project): Unit = with(project) {
        configureIdentity()
        pluginManager.apply("base")
        configureRootPomPublication()

        tasks.named("check") {
            // Et inkludert bygg omfattes ikke automatisk av rotprosjektets check.
            dependsOn(gradle.includedBuild("build-logic").task(":check"))
        }

        tasks.register("publicationArtifacts") {
            group = "build"
            description = "Build all publication artifacts without publishing anything."
            dependsOn("generatePomFileForMorPomPublication")
            dependsOn(subprojects.map { "${it.path}:publicationArtifacts" })
        }
    }
}
