package no.nav.common.build

import groovy.util.Node
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.the

/** Verdier fanges ved konfigurering, slik at POM-generering ikke trenger Project. */
internal data class MavenParentMetadata(
    val javaVersion: String,
    val properties: Map<String, String>,
    val dependencies: List<MavenParentDependency>,
)

internal data class MavenParentDependency(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val scope: String? = null,
    val optional: Boolean = false,
    val excludeTransitives: Boolean = false,
)

private data class ManagedVersion(
    val property: String,
    val catalogAlias: String,
    val libraries: List<String>,
)

private fun managedVersion(
    property: String,
    catalogAlias: String,
    vararg libraries: String,
) = ManagedVersion(property, catalogAlias, libraries.toList())

private fun managedVersionForAlias(
    alias: String,
    vararg additionalLibraries: String,
) = ManagedVersion(alias, alias, listOf(alias) + additionalLibraries)

private const val CXF_JAXWS_BLUEPRINT = "cxf-jaxws-blueprint"
private const val MAVEN_JAVA_VERSION = $$"${java.version}"
private const val MAVEN_SOURCE_PLUGIN = "maven-source-plugin"

// Oppføringene under er den arvede Maven-kontrakten, ikke en liste over Gradle-avhengigheter.
private val PARENT_VERSIONS = listOf(
    managedVersion("slf4j", "slf4j", "slf4j-api", "slf4j-simple", "jcl-over-slf4j"),
    managedVersion("logback", "logback", "logback-classic", "logback-core"),
    managedVersionForAlias("logback-syslog4j"),
    managedVersionForAlias("janino"),
    managedVersionForAlias("logstash-logback"),
    managedVersionForAlias(CXF_JAXWS_BLUEPRINT),
    managedVersionForAlias(
        "cxf-core",
        "cxf-ws-policy",
        "cxf-transports-http",
        "cxf-bindings-soap",
        "cxf-ws-security",
        "cxf-features-logging",
        "cxf-frontend-jaxws",
        "cxf-frontend-simple",
    ),
    managedVersionForAlias("jsoup"),
    managedVersionForAlias("lombok"),
    managedVersionForAlias("jakarta.servlet"),
    managedVersionForAlias("jakarta.ws.rs-api"),
    managedVersion("spring", "spring", "spring-web", "spring-jdbc", "spring-test"),
    managedVersion(
        "jersey",
        "jersey",
        "jersey-container-servlet",
        "jersey-server",
        "jersey-hk2",
        "jersey-media-json-jackson",
        "jersey-apache-connector",
    ),
    managedVersion(
        "jackson",
        "jackson",
        "jackson-provider",
        "jackson-xmlbind",
        "jackson-databind",
        "jackson-core",
        "jackson-yaml",
        "jackson-kotlin",
    ),
    managedVersionForAlias("jackson-annotations"),
    managedVersionForAlias("jaxb-api"),
    managedVersionForAlias("micrometer"),
    managedVersion("okhttp", "okhttp", "okhttp", "mockwebserver"),
    managedVersion("prometheus", "prometheus", "prometheus-logback"),
    managedVersionForAlias("commons-lang"),
    managedVersionForAlias("commons-io"),
    managedVersionForAlias("shedlock"),
    managedVersionForAlias("handlebars"),
    managedVersionForAlias("gson"),
    managedVersionForAlias("caffeine"),
    managedVersion("bitbucket-jose4", "bitbucket-jose4", "jose4j"),
    managedVersion("nimbusds", "nimbusds", "oauth2-oidc-sdk"),
    managedVersionForAlias("unleash"),
    managedVersion("org.json", "org-json", "org-json"),
    managedVersion("jetty", "jetty", "jetty-server", "jetty-webapp", "jetty-servlet", "jetty-annotations"),
    managedVersion("kafka", "kafka", "kafka-clients"),
    managedVersionForAlias("avro"),
    managedVersionForAlias("avro-serializer"),
    managedVersion("json-schema-validation", "json-schema-validation", "json-schema"),
    managedVersionForAlias("assertj"),
    managedVersion("hamcrest", "hamcrest", "hamcrest-library", "hamcrest-core"),
    managedVersionForAlias("mockito"),
    managedVersion("junit.jupiter", "junit-jupiter", "junit-vintage", "junit-api", "junit-engine"),
    managedVersionForAlias("wiremock"),
    managedVersion(
        "testcontainers",
        "testcontainers",
        "testcontainers-core",
        "testcontainers-kafka",
        "testcontainers-postgresql",
    ),
    managedVersionForAlias("h2"),
    managedVersion("postgres", "postgres", "postgresql"),
    managedVersionForAlias("bytebuddy"),
)

private val TEST_MANAGEMENT = setOf(
    "spring-test",
    CXF_JAXWS_BLUEPRINT,
    "mockwebserver",
    "wiremock",
    "mockito",
    "bytebuddy",
    "junit-vintage",
    "junit-api",
    "junit-engine",
    "hamcrest-library",
    "assertj",
    "testcontainers-core",
    "testcontainers-kafka",
    "testcontainers-postgresql",
    "h2",
    "postgresql",
    "awaitility",
)

private val RUNTIME_MANAGEMENT = setOf(
    "jersey-media-json-jackson",
    "logback-classic",
    "jcl-over-slf4j",
    "janino",
)

internal fun Project.createParentPomMetadata(): MavenParentMetadata {
    val catalog = the<VersionCatalogsExtension>().named("libs")
    val properties = linkedMapOf<String, String>()
    val managed = mutableListOf<MavenParentDependency>()

    fun dependency(
        alias: String,
        version: String,
    ): MavenParentDependency {
        val library = catalog.findLibrary(alias).orElseThrow { IllegalArgumentException("Mangler libs.$alias") }.get()
        return MavenParentDependency(
            groupId = library.module.group,
            artifactId = library.module.name,
            version = version,
            scope = when (alias) {
                in TEST_MANAGEMENT -> "test"
                in RUNTIME_MANAGEMENT -> "runtime"
                "lombok" -> "provided"
                else -> null
            },
            optional = alias in setOf("spring-web", "spring-jdbc"),
            excludeTransitives = alias == CXF_JAXWS_BLUEPRINT,
        )
    }

    // Disse fire ble styrt med faste versjoner, ikke via parent-properties.
    managed += MavenParentDependency("commons-collections", "commons-collections4", libraryVersion("commons-collections4"))
    listOf("fastinfoset", "istack", "jaxb-runtime").forEach { alias ->
        managed += dependency(
            alias,
            catalog
                .findLibrary(alias)
                .get()
                .get()
                .versionConstraint.requiredVersion,
        )
    }

    PARENT_VERSIONS.forEach { entry ->
        properties["${entry.property}.version"] = libraryVersion(entry.catalogAlias)
        entry.libraries.forEach { alias -> managed += dependency(alias, $$"${$${entry.property}.version}") }
    }
    managed += dependency("awaitility", libraryVersion("awaitility"))

    // Disse ble introdusert i Gradle-migreringen og må også beholdes i parent-POM-en.
    listOf("jackson2-core", "jackson2-databind", "jackson2-csv", "jackson2-jdk8", "kotlin-stdlib", "kotlin-reflect")
        .forEach { alias ->
            val version = catalog
                .findLibrary(alias)
                .get()
                .get()
                .versionConstraint.requiredVersion
            managed += dependency(alias, version)
        }

    return MavenParentMetadata(libraryVersion("java-version"), properties, managed)
}

internal fun Node.appendMavenExclusion(
    group: String,
    artifact: String,
) {
    val exclusion = appendNode("exclusions").appendNode("exclusion")
    exclusion.appendNode("groupId", group)
    exclusion.appendNode("artifactId", artifact)
}

private fun Node.mavenPlugin(
    artifact: String,
    version: String? = null,
    group: String? = null,
): Node = appendNode("plugin").also { plugin ->
    group?.let { plugin.appendNode("groupId", it) }
    plugin.appendNode("artifactId", artifact)
    version?.let { plugin.appendNode("version", it) }
}

private fun Node.mavenExecution(
    id: String? = null,
    phase: String? = null,
    goal: String,
): Node = appendNode("executions").appendNode("execution").also { execution ->
    id?.let { execution.appendNode("id", it) }
    phase?.let { execution.appendNode("phase", it) }
    execution.appendNode("goals").appendNode("goal", goal)
}

/** Arvet Maven-oppsett beholdes for Maven-barn, men kjøres ikke av Gradle. */
internal fun Node.addMavenParentBuild() {
    val build = appendNode("build")
    val management = build.appendNode("pluginManagement").appendNode("plugins")
    management.mavenPlugin("maven-clean-plugin", "3.5.0")
    management.mavenPlugin("maven-resources-plugin", "3.5.0")
    management.mavenPlugin("maven-compiler-plugin", "3.16.0").appendNode("configuration").also {
        it.appendNode("source", MAVEN_JAVA_VERSION)
        it.appendNode("target", MAVEN_JAVA_VERSION)
    }
    management.mavenPlugin("maven-install-plugin", "3.2.0")
    management.mavenPlugin(MAVEN_SOURCE_PLUGIN, "3.4.0").mavenExecution(goal = "test-jar")
    management.mavenPlugin("maven-jar-plugin", "3.5.1").also { plugin ->
        plugin.mavenExecution(goal = "test-jar")
        plugin
            .appendNode("configuration")
            .appendNode("archive")
            .appendNode("manifest")
            .appendNode("addDefaultImplementationEntries", "true")
    }

    val plugins = build.appendNode("plugins")
    plugins.mavenPlugin(MAVEN_SOURCE_PLUGIN).mavenExecution(id = "attach-sources", goal = "jar")
    plugins.mavenPlugin("jacoco-maven-plugin", "0.8.16", "org.jacoco")
    plugins.mavenPlugin("flatten-maven-plugin", "1.8.0", "org.codehaus.mojo").also { plugin ->
        val configuration = plugin.appendNode("configuration")
        configuration.appendNode("updatePomFile", "true")
        configuration.appendNode("flattenMode", "resolveCiFriendliesOnly")
        val executions = plugin.appendNode("executions")
        executions.appendNode("execution").also { execution ->
            execution.appendNode("id", "flatten")
            execution.appendNode("phase", "process-resources")
            execution.appendNode("goals").appendNode("goal", "flatten")
        }
        executions.appendNode("execution").also { execution ->
            execution.appendNode("id", "flatten.clean")
            execution.appendNode("phase", "clean")
            execution.appendNode("goals").appendNode("goal", "clean")
        }
    }

    val profiles = appendNode("profiles")
    val release = profiles.appendNode("profile")
    release.appendNode("id", "release")
    val releasePlugins = release.appendNode("build").appendNode("plugins")
    releasePlugins.mavenPlugin(MAVEN_SOURCE_PLUGIN).mavenExecution(id = "attach-sources", goal = "jar-no-fork")
    releasePlugins.mavenPlugin("maven-javadoc-plugin", "3.12.0", "org.apache.maven.plugins").also { plugin ->
        plugin.appendNode("configuration").appendNode("source", MAVEN_JAVA_VERSION)
        plugin.mavenExecution(id = "attach-javadocs", goal = "jar")
    }

    val coverage = profiles.appendNode("profile")
    coverage.appendNode("id", "coverage")
    val jacoco = coverage.appendNode("build").appendNode("plugins").mavenPlugin("jacoco-maven-plugin", group = "org.jacoco")
    val executions = jacoco.appendNode("executions")
    executions.appendNode("execution").also { execution ->
        execution.appendNode("id", "jacoco-prepare-agent")
        execution.appendNode("goals").appendNode("goal", "prepare-agent")
    }
    executions.appendNode("execution").also { execution ->
        execution.appendNode("id", "post-test")
        execution.appendNode("phase", "test")
        execution.appendNode("configuration").appendNode("formats").appendNode("format", "XML")
        execution.appendNode("goals").appendNode("goal", "report")
    }
}
