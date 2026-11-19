package no.nav.common.build

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.nio.file.Files

/** Velger Java-versjonen fra katalogen og registrerer kilde- og eventuelt Javadoc-artifakter. */
internal fun Project.configureJavaToolchain() {
    val javaVersion = libraryVersion("java-version").toInt()

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
        withSourcesJar()
        if (providers.gradleProperty("release").isPresent) withJavadocJar()
    }
}

/** Konfigurerer Java-kompilering, manifestet og testartifaktene som bygges med biblioteket. */
internal fun Project.configureJavaArtifacts() {
    val javaVersion = libraryVersion("java-version")

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(javaVersion.toInt())
    }

    val java = the<JavaPluginExtension>()
    val testSourceSet = java.sourceSets.named("test").get()
    val testsJar = tasks.register<Jar>("testsJar") {
        archiveClassifier.set("tests")
        from(testSourceSet.output)
    }
    tasks.register<Jar>("testSourcesJar") {
        archiveClassifier.set("test-sources")
        from(testSourceSet.allSource)
    }

    configurations.create("testArtifacts") {
        isCanBeConsumed = true
        isCanBeResolved = false
        // Delte testklasser konsumeres i dette bygget, uavhengig av Maven-klassifiserte artefakter.
        extendsFrom(configurations.named("api").get(), configurations.named("runtimeOnly").get())
        outgoing.artifact(testsJar)
    }

    tasks.withType<Jar>().configureEach {
        manifest.attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version.toString(),
                "Java-Version" to javaVersion,
                "Build-Jdk-Spec" to javaVersion,
            ),
        )
    }
}

/** Registrerer Maven-repositoriene som bibliotekmodulene trenger. */
internal fun Project.configureLibraryRepositories() {
    repositories.mavenCentral()

    // Gradle leser ikke repository-deklarasjonen fra WSS4J sin Maven-POM.
    repositories.maven("https://repository.jboss.org/nexus/content/groups/public/") {
        name = "JBossOpenSaml"
        content {
            includeGroup("org.opensaml")
            includeGroupByRegex("net\\.shibboleth.*")
        }
    }

    if (name == "kafka") {
        repositories.maven("https://packages.confluent.io/maven/")
    }
}

/** Konfigurerer testoppdagelse, JUnit, JaCoCo og Javadoc for bibliotekmoduler. */
internal fun Project.configureLibraryTests() {
    dependencies.add(
        "testRuntimeOnly",
        "org.junit.platform:junit-platform-launcher:${libraryVersion("junit.jupiter")}",
    )

    tasks.withType<Test>().configureEach {
        val testTempDir = temporaryDir
        systemProperty("java.io.tmpdir", testTempDir.absolutePath)
        doFirst { Files.createDirectories(testTempDir.toPath()) }

        useJUnitPlatform()
        // Samme oppdagelse som Surefire; ikke aktiver testhjelpere som ikke var valgt tidligere.
        include("**/Test*.class", "**/*Test.class", "**/*Tests.class", "**/*TestCase.class")
        maxParallelForks = 1
        testLogging.showStandardStreams = false

        extensions.configure<JacocoTaskExtension> {
            isEnabled = providers.gradleProperty("coverage").isPresent
        }
    }

    extensions.configure<JacocoPluginExtension> {
        toolVersion = libraryVersion("jacoco")
    }

    tasks.withType<JacocoReport>().configureEach {
        dependsOn(tasks.named("test"))
        reports.xml.required.set(true)
    }

    tasks.withType<Javadoc>().configureEach {
        options.encoding = "UTF-8"
    }
}

/**
 * Kobler deklarert Lombok til begge annotasjonsprosessorene og testdeklarert Mockito til en Java-agent.
 *
 * Avhengighetene kopieres slik at versjoner og exclusions fra modulens deklarasjon beholdes.
 * Oppsettet holdes separat fra Maven-metadataene fordi det styrer kompilering og testkjøring.
 */
internal fun Project.configureLibraryDependencyTools() {
    val state = DependencyToolingState()
    val dependencyConfigurations = listOf("api", "implementation", "runtimeOnly", "compileOnly") + TEST_CONFIGURATIONS

    dependencyConfigurations.forEach { configurationName ->
        configurations.named(configurationName).configure {
            dependencies.withType<ModuleDependency>().all {
                registerLombokProcessors(this@configureLibraryDependencyTools, this, state)
                registerMockitoAgent(this@configureLibraryDependencyTools, configurationName, this, state)
            }
        }
    }
}

private val TEST_CONFIGURATIONS = setOf("testImplementation", "testCompileOnly")

private class DependencyToolingState {
    val seenLombok = mutableSetOf<String>()
    val seenMockito = mutableSetOf<String>()
    var mockitoAgentConfiguration: org.gradle.api.artifacts.Configuration? = null
    var mockitoAgentAttachedToTests = false
}

private fun ModuleDependency.dependencyKey() = "${group.orEmpty()}:$name:${version.orEmpty()}:${excludeRules.hashCode()}"

private fun registerLombokProcessors(
    project: Project,
    dependency: ModuleDependency,
    state: DependencyToolingState,
) {
    // Speil Lombok bare én gang; Gradle kan eksponere tilsvarende deklarasjoner fra flere konfigurasjoner.
    if ((dependency.group != "org.projectlombok" || dependency.name != "lombok") ||
        !state.seenLombok.add(dependency.dependencyKey())
    ) {
        return
    }

    project.dependencies.add("annotationProcessor", dependency.copy())
    project.dependencies.add("testAnnotationProcessor", dependency.copy())
}

private fun registerMockitoAgent(
    project: Project,
    configurationName: String,
    dependency: ModuleDependency,
    state: DependencyToolingState,
) {
    if (configurationName !in TEST_CONFIGURATIONS) return
    if (dependency.group != "org.mockito" || dependency.name != "mockito-core") return

    val agent = ensureMockitoAgentConfiguration(project, state)
    if (state.seenMockito.add(dependency.dependencyKey())) {
        project.dependencies.add(agent.name, dependency.copy())
    }
    if (state.mockitoAgentAttachedToTests) return

    project.tasks.withType<Test>().configureEach {
        jvmArgumentProviders.add(MockitoAgent(agent))
    }
    state.mockitoAgentAttachedToTests = true
}

private fun ensureMockitoAgentConfiguration(
    project: Project,
    state: DependencyToolingState,
): org.gradle.api.artifacts.Configuration {
    if (state.mockitoAgentConfiguration == null) {
        state.mockitoAgentConfiguration =
            project.configurations.create("mockitoAgent") {
                isCanBeConsumed = false
                isCanBeResolved = true
                isTransitive = false
            }
    }
    return requireNotNull(state.mockitoAgentConfiguration)
}

/** Registrerer oppgaven som skriver løste avhengigheter til rapportfiler. */
internal fun Project.registerDependencySnapshot() {
    tasks.register<DependencySnapshot>("dependencySnapshot") {
        group = "help"
        description = "Write resolved external coordinates for migration comparisons."
        destination.set(layout.buildDirectory.dir("reports/dependencies"))

        listOf("compileClasspath", "runtimeClasspath", "testCompileClasspath", "testRuntimeClasspath")
            .forEach { configurationName ->
                val configuration = configurations.named(configurationName).get()

                // Provideren utsetter oppløsning til rapportoppgaven trenger sine input.
                coordinates.put(
                    configurationName,
                    configuration.incoming.artifacts.resolvedArtifacts.map { artifacts ->
                        artifacts
                            .mapNotNull {
                                val id = it.id.componentIdentifier as? ModuleComponentIdentifier
                                id?.let { module -> "${module.group}:${module.module}:${module.version}" }
                            }.toSortedSet()
                            .toList()
                    },
                )
            }
    }
}

/** Skriver løste eksterne avhengigheter per konfigurasjon til utdatafiler. */
@CacheableTask
abstract class DependencySnapshot : DefaultTask() {
    /** Sorterte koordinater per klassebane, uten Gradle-prosjektobjekter i oppgavens input. */
    @get:Input
    abstract val coordinates: MapProperty<String, List<String>>

    /** Rapportkatalog med én tekstfil per klassebane. */
    @get:OutputDirectory
    abstract val destination: DirectoryProperty

    /** Oppretter en tekstfil med koordinatene for hver avhengighetskonfigurasjon. */
    @TaskAction
    fun generate() {
        val directory = destination.get().asFile
        directory.mkdirs()

        coordinates.get().forEach { (configuration, dependencies) ->
            directory
                .resolve("$configuration.txt")
                .writeText(dependencies.joinToString("\n", postfix = "\n"))
        }
    }
}
