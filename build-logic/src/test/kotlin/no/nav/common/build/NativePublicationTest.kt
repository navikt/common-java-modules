package no.nav.common.build

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.io.File
import java.util.UUID
import java.util.jar.JarFile
import javax.xml.parsers.DocumentBuilderFactory

/** Tester at native Gradle-deklarasjoner beholder publiserings- og klassebaneatferd. */
class NativePublicationTest {
    /** Kontrollerer POM-er, testverktøy, JAR-metadata og konfigurasjonsbufferen i et eksempelprosjekt. */
    @Test
    fun `native-deklarasjoner bevarer publiseringens klassebaner og testverktøy med konfigurasjons-cache`() {
        // Arrange
        val directory = File("build/publication-tests/${UUID.randomUUID()}").absoluteFile.apply { mkdirs() }
        try {
            File(directory, "settings.gradle.kts").writeText(
                """
                rootProject.name = "fixture"
                include("consumer", "producer", "gradleConsumer")
                """.trimIndent(),
            )
            File(directory, "gradle").mkdirs()
            File("../gradle/libs.versions.toml").copyTo(File(directory, "gradle/libs.versions.toml"))
            File(directory, "build.gradle.kts").writeText("""plugins { id("nav.common-root") }""")
            File(directory, "producer").mkdirs()
            File(directory, "producer/build.gradle.kts").writeText("""plugins { id("nav.common-library") }""")
            File(directory, "producer/src/main/java/fixture").mkdirs()
            File(directory, "producer/src/main/java/fixture/Marker.java")
                .writeText("package fixture; public class Marker {}")
            File(directory, "producer/src/test/java/fixture").mkdirs()
            File(directory, "producer/src/test/java/fixture/TestData.java")
                .writeText("package fixture; public class TestData {}")
            File(directory, "gradleConsumer").mkdirs()
            File(directory, "gradleConsumer/build.gradle.kts").writeText(
                """
                plugins { id("nav.common-library") }
                dependencies {
                    api(project(":producer"))
                    testImplementation(project(path = ":producer", configuration = "testArtifacts"))
                }
                tasks.register<Copy>("copyCompileDependencies") {
                    from(configurations["compileClasspath"])
                    into(layout.buildDirectory.dir("compile-dependencies"))
                }
                tasks.register<Copy>("copyTestDependencies") {
                    from(configurations["testRuntimeClasspath"])
                    into(layout.buildDirectory.dir("test-dependencies"))
                }
                """.trimIndent(),
            )
            File(directory, "consumer").mkdirs()
            File(directory, "consumer/build.gradle.kts").writeText(
                """
                import no.nav.common.build.mavenOptional
                plugins { id("nav.common-library") }
                dependencies {
                    api(project(":producer")) { exclude(group = "excluded", module = "library") }
                    api(libs.logback.classic)
                    runtimeOnly(libs.jcl.over.slf4j)
                    compileOnly(libs.lombok)
                    compileOnly(libs.micrometer) { mavenOptional() }
                    testImplementation(libs.mockito)
                    testCompileOnly(libs.mockito)
                    testImplementation(libs.spring.web)
                    testImplementation(libs.cxf.jaxws.blueprint)
                    testImplementation(project(path = ":producer", configuration = "testArtifacts"))
                    testImplementation("example:classified:1.2:custom") {
                        exclude(group = "excluded", module = "library")
                    }
                    constraints { api("example:transitive:1.0") }
                }
                val optional = configurations["optionalRuntime"].dependencies
                check(optional.map { it.name } == listOf("micrometer-core"))
                check(configurations["runtimeClasspath"].allDependencies.none { it.name == "lombok" })
                listOf("testCompileClasspath", "testRuntimeClasspath").forEach { name ->
                    check(configurations[name].allDependencies.any { it.name == "lombok" })
                    check(configurations[name].allDependencies.any { it.name == "micrometer-core" })
                }
                check(configurations["apiElements"].allDependencies.none { it.name == "micrometer-core" })
                check(configurations["runtimeElements"].allDependencies.none { it.name == "micrometer-core" })
                listOf("annotationProcessor", "testAnnotationProcessor").forEach { name ->
                    check(configurations[name].dependencies.single().name == "lombok")
                }
                check(!configurations["mockitoAgent"].isTransitive)
                check(configurations["mockitoAgent"].dependencies.single().name == "mockito-core")
                check(tasks.named<Test>("test").get().jvmArgumentProviders.any {
                    it is no.nav.common.build.MockitoAgent
                })
                """.trimIndent(),
            )
            val runner = GradleRunner
                .create()
                .withProjectDir(directory)
                .withTestKitDir(File("build/test-kit").absoluteFile)
                .withPluginClasspath()
                .withArguments(
                    ":consumer:generatePomFileForMavenJavaPublication",
                    ":consumer:sourcesJar",
                    ":gradleConsumer:copyCompileDependencies",
                    ":gradleConsumer:copyTestDependencies",
                    "-Prevision=4.test",
                    "--configuration-cache",
                    "--stacktrace",
                )

            // Act
            runner.build()
            val secondBuild = runner.build()
            val reusedConfigurationCache = secondBuild.output.contains("Reusing configuration cache.")
            val selectedTasks = GradleRunner
                .create()
                .withProjectDir(directory)
                .withPluginClasspath()
                .withArguments("publicationArtifacts", "dependencyUpdates", "--dry-run", "--stacktrace")
                .build()
                .output

            // Assert
            listOf("consumer", "producer", "gradleConsumer").forEach { module ->
                assertTrue(selectedTasks.contains(":$module:publicationArtifacts SKIPPED"))
                assertTrue(selectedTasks.contains(":$module:dependencyUpdates SKIPPED"))
            }
            assertTrue(selectedTasks.contains(":generatePomFileForMorPomPublication SKIPPED"))
            val compile = File(directory, "gradleConsumer/build/compile-dependencies")
            val tests = File(directory, "gradleConsumer/build/test-dependencies")
            assertTrue(File(compile, "fixture/Marker.class").isFile, "Gradle API consumer must see producer's classes")
            assertFalse(File(compile, "fixture/TestData.class").exists())
            assertTrue(
                tests.listFiles().orEmpty().any { file ->
                    file.name == "producer-4.test-tests.jar" &&
                        JarFile(file).use { it.getJarEntry("fixture/TestData.class") != null }
                },
                "Gradle test consumer must see producer's test JAR: ${tests.listFiles().orEmpty().map { it.name }}",
            )

            val pomFile = File(directory, "consumer/build/publications/mavenJava/pom-default.xml")
            val pom = parse(pomFile)
            assertEquals("no.nav.common", pom.text("groupId"))
            assertEquals("4.test", pom.text("version"))
            assertEquals("", pom.text("parent"))
            assertEquals("Common Java Modules", pom.text("name"))
            assertEquals(
                "MIT License",
                pom
                    .direct("licenses")
                    .single()
                    .direct("license")
                    .single()
                    .text("name"),
            )
            val managed = pom
                .direct("dependencyManagement")
                .single()
                .direct("dependencies")
                .single()
                .direct("dependency")
            val managedCoordinates = managed.map { "${it.text("groupId")}:${it.text("artifactId")}" }.toSet()
            assertTrue(
                managedCoordinates.containsAll(
                    setOf(
                        "example:transitive",
                        "ch.qos.logback:logback-classic",
                        "org.json:json",
                        "com.fasterxml.jackson.core:jackson-databind",
                        "net.bytebuddy:byte-buddy",
                    ),
                ),
            )
            assertEquals(
                "1.0",
                managed.single { it.text("artifactId") == "transitive" }.text("version"),
            )
            val fixtureCatalog = File(directory, "gradle/libs.versions.toml")
            assertEquals(
                fixtureCatalog.fixtureCatalogVersion("jackson2"),
                managed.single { it.text("artifactId") == "jackson-databind" }.text("version"),
            )
            assertEquals(
                fixtureCatalog.fixtureCatalogVersion("bytebuddy"),
                managed.single { it.text("artifactId") == "byte-buddy" }.text("version"),
            )
            val dependencies = pom.direct("dependencies").single().direct("dependency")
            assertTrue(dependencies.size >= 6)

            /** Finner POM-avhengigheten med angitt artefakt og type. */
            fun dependency(
                artifact: String,
                type: String = "",
            ) = dependencies.single { it.text("artifactId") == artifact && it.text("type") == type }
            fun dependencyWithScope(
                artifact: String,
                scope: String,
            ) = dependencies.single { it.text("artifactId") == artifact && it.text("scope") == scope }
            assertTrue(reusedConfigurationCache)
            assertEquals("compile", dependency("logback-classic").text("scope"))
            assertEquals(
                dependency("logback-classic").text("version"),
                managed.single { it.text("artifactId") == "logback-classic" }.text("version"),
            )
            assertEquals("runtime", dependency("jcl-over-slf4j").text("scope"))
            assertEquals("compile", dependency("micrometer-core").text("scope"))
            assertEquals("true", dependency("micrometer-core").text("optional"))
            assertEquals("4.test", dependencyWithScope("producer", "compile").text("version"))
            assertEquals("test", dependency("producer", "test-jar").text("scope"))
            assertEquals("4.test", dependency("producer", "test-jar").text("version"))
            assertEquals("provided", dependencyWithScope("lombok", "provided").text("scope"))
            assertEquals("test", dependencyWithScope("mockito-core", "test").text("scope"))
            assertEquals(
                "excluded",
                dependencyWithScope("producer", "compile")
                    .direct("exclusions")
                    .single()
                    .direct("exclusion")
                    .single()
                    .text("groupId"),
            )

            assertFalse(File(directory, "build/publications").exists())
            assertFalse(File(directory, "consumer/build/publications/mavenJava/module.json").exists())
            JarFile(File(directory, "consumer/build/libs/consumer-4.test-sources.jar")).use { jar ->
                val prefix = "META-INF/maven/no.nav.common/consumer/"
                assertEquals(
                    pomFile.readText(),
                    jar.getInputStream(jar.getJarEntry("${prefix}pom.xml")).reader().readText(),
                )
                assertTrue(
                    jar
                        .getInputStream(jar.getJarEntry("${prefix}pom.properties"))
                        .reader()
                        .readText()
                        .contains("version=4.test"),
                )
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    /** Leser POM-filen som et XML-dokument. */
    private fun parse(file: File): Element = DocumentBuilderFactory
        .newInstance()
        .newDocumentBuilder()
        .parse(file)
        .documentElement

    /** Returnerer direkte underelementer med det angitte navnet. */
    private fun Element.direct(name: String): List<Element> =
        (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }.filter { it.tagName == name }

    /** Henter tekstinnholdet i ett direkte underelement, eller tom tekst hvis det mangler. */
    private fun Element.text(name: String) = direct(name).singleOrNull()?.textContent.orEmpty()
}
