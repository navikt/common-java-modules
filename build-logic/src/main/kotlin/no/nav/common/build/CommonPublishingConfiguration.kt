package no.nav.common.build

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.WriteProperties
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

/** Publiserer Java-komponenten, inkludert eksplisitt valgfrie avhengigheter. */
internal fun Project.createCommonPublication(): MavenPublication {
    val javaComponent = components.named<AdhocComponentWithVariants>("java").get()
    javaComponent.addVariantsFromConfiguration(configurations.named("optionalRuntimePublication").get()) {
        mapToMavenScope("compile")
        mapToOptional()
    }

    return the<PublishingExtension>()
        .publications
        .create<MavenPublication>("mavenJava") {
            from(javaComponent)
            artifact(tasks.named("testsJar"))
            artifact(tasks.named("testSourcesJar"))
        }
}

internal fun Project.configureCommonPublicationPom(publication: MavenPublication) {
    val supplementalDependencies = linkedMapOf<String, PomDependency>()
    registerPomScopedDependencies(supplementalDependencies, "compileOnly", "provided") { !it.isMavenOptional }
    listOf("testImplementation", "testCompileOnly", "testRuntimeOnly")
        .forEach { configurationName ->
            registerPomScopedDependencies(supplementalDependencies, configurationName, "test")
        }

    publication.pom.apply {
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
        withXml {
            if (supplementalDependencies.isEmpty()) return@withXml
            val projectNode = asNode()
            val dependenciesNode =
                (projectNode.get("dependencies") as? groovy.util.NodeList)
                    ?.firstOrNull() as? groovy.util.Node ?: projectNode.appendNode("dependencies")
            supplementalDependencies.values.forEach { dependency ->
                val dependencyNode = dependenciesNode.appendNode("dependency")
                dependencyNode.appendNode("groupId", dependency.groupId)
                dependencyNode.appendNode("artifactId", dependency.artifactId)
                dependency.version?.let { dependencyNode.appendNode("version", it) }
                dependency.type?.let { dependencyNode.appendNode("type", it) }
                dependency.classifier?.let { dependencyNode.appendNode("classifier", it) }
                dependencyNode.appendNode("scope", dependency.scope)
                if (dependency.exclusions.isNotEmpty()) {
                    val exclusionsNode = dependencyNode.appendNode("exclusions")
                    dependency.exclusions.forEach { exclusion ->
                        val exclusionNode = exclusionsNode.appendNode("exclusion")
                        exclusionNode.appendNode("groupId", exclusion.group)
                        exclusionNode.appendNode("artifactId", exclusion.module)
                    }
                }
            }
        }
    }
}

private data class PomExclusion(
    val group: String,
    val module: String,
)

private fun exclusionPart(value: Any?): String = value?.toString()?.takeIf { it.isNotBlank() } ?: "*"

private fun org.gradle.api.artifacts.ExcludeRule.toPomExclusion(): PomExclusion = PomExclusion(
    group = exclusionPart(group),
    module = exclusionPart(module),
)

private data class PomDependency(
    val groupId: String,
    val artifactId: String,
    val version: String?,
    val type: String?,
    val classifier: String?,
    val scope: String,
    val exclusions: Set<PomExclusion>,
) {
    val identityKey: String
        get() =
            listOf(
                groupId,
                artifactId,
                version.orEmpty(),
                type.orEmpty(),
                classifier.orEmpty(),
                scope,
                exclusions.sortedBy { "${it.group}:${it.module}" }.joinToString(",") { "${it.group}:${it.module}" },
            ).joinToString("|")
}

@Suppress("ObjectLiteralToLambda")
private fun Project.registerPomScopedDependencies(
    dependenciesByIdentity: MutableMap<String, PomDependency>,
    configurationName: String,
    scope: String,
    include: (ModuleDependency) -> Boolean = { true },
) {
    val publicationVersion = version.toString()
    configurations.named(configurationName).configure {
        fun captureDependency(dependency: Any) {
            if (dependency is ModuleDependency && !include(dependency)) return
            val pomDependency = when (dependency) {
                is ProjectDependency ->
                    PomDependency(
                        groupId = COMMON_GROUP,
                        artifactId = dependency.path.substringAfterLast(':'),
                        version = publicationVersion,
                        type = if (dependency.targetConfiguration == "testArtifacts") "test-jar" else null,
                        classifier = null,
                        scope = scope,
                        exclusions = dependency.excludeRules.map { it.toPomExclusion() }.toSet(),
                    )

                is ModuleDependency -> dependency.toPomDependency(scope)

                else -> null
            } ?: return
            dependenciesByIdentity[pomDependency.identityKey] = pomDependency
        }

        dependencies.all(
            object : org.gradle.api.Action<Dependency> {
                override fun execute(dependency: Dependency) {
                    captureDependency(dependency)
                }
            },
        )
    }
}

private fun ModuleDependency.toPomDependency(scope: String): PomDependency? {
    val groupId = group ?: return null
    val artifactId = name
    val artifact = artifacts.firstOrNull()
    val type = artifact?.type?.takeUnless { it == "jar" }
    return PomDependency(
        groupId = groupId,
        artifactId = artifactId,
        version = version,
        type = type,
        classifier = artifact?.classifier,
        scope = scope,
        exclusions = excludeRules.map { it.toPomExclusion() }.toSet(),
    )
}

internal fun Project.configureGitHubPackagesRepository() {
    the<PublishingExtension>().repositories.maven("https://maven.pkg.github.com/navikt/common-java-modules") {
        name = "GitHubPackages"
        credentials {
            username = providers.environmentVariable("GITHUB_ACTOR").orNull
            password = providers.environmentVariable("GITHUB_TOKEN").orNull
        }
    }
}

/** Arkivskannere trenger Maven-koordinater også når de ikke kan inspisere bygget. */
internal fun Project.embedMavenPublicationMetadata() {
    val mavenProperties = tasks.register<WriteProperties>("generateMavenProperties") {
        destinationFile.set(layout.buildDirectory.file("generated/maven/pom.properties"))
        property("artifactId", project.name)
        property("groupId", project.group.toString())
        property("version", project.version.toString())
    }

    val descriptorDirectory = "META-INF/maven/$group/$name"
    tasks.withType<Jar>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        from(tasks.named("generatePomFileForMavenJavaPublication")) {
            into(descriptorDirectory)
            rename { "pom.xml" }
        }
        from(mavenProperties) { into(descriptorDirectory) }
    }
}
