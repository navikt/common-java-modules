package no.nav.common.build

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.io.File
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory

/** Tester at mor-pom publiserer parent-kontrakten med dependencyManagement og profiler. */
class RootPomPublicationTest {
    @Test
    fun `publicationArtifacts bygger mor-pom uten moduler`() {
        val directory = File("build/publication-tests/${UUID.randomUUID()}").absoluteFile.apply { mkdirs() }
        try {
            File(directory, "settings.gradle.kts").writeText("""rootProject.name = "fixture" """)
            File(directory, "gradle").mkdirs()
            File("../gradle/libs.versions.toml").copyTo(File(directory, "gradle/libs.versions.toml"))
            File(directory, "build.gradle.kts").writeText("""plugins { id("nav.common-root") }""")

            GradleRunner
                .create()
                .withProjectDir(directory)
                .withTestKitDir(File("build/test-kit").absoluteFile)
                .withPluginClasspath()
                .withArguments("publicationArtifacts", "-Prevision=4.test")
                .build()

            assertTrue(File(directory, "build/publications/morPom/pom-default.xml").isFile)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `mor-pom beholder parent metadata og støtter konfigurasjons-cache`() {
        // Arrange
        val directory = File("build/publication-tests/${UUID.randomUUID()}").absoluteFile.apply { mkdirs() }
        try {
            File(directory, "settings.gradle.kts").writeText(
                """
                rootProject.name = "fixture"
                include("auth", "test", "util")
                """.trimIndent(),
            )
            File(directory, "gradle").mkdirs()
            File("../gradle/libs.versions.toml").copyTo(File(directory, "gradle/libs.versions.toml"))
            File(directory, "build.gradle.kts").writeText("""plugins { id("nav.common-root") }""")
            File(directory, "auth").mkdirs()
            File(directory, "test").mkdirs()
            File(directory, "util").mkdirs()

            val runner =
                GradleRunner
                    .create()
                    .withProjectDir(directory)
                    .withTestKitDir(File("build/test-kit").absoluteFile)
                    .withPluginClasspath()
                    .withArguments("generatePomFileForMorPomPublication", "-Prevision=4.test", "--configuration-cache")

            // Act
            runner.build()
            val secondBuild = runner.build()
            val reusedConfigurationCache = secondBuild.output.contains("Reusing configuration cache.")

            // Assert
            val pomFile = File(directory, "build/publications/morPom/pom-default.xml")
            val pom = parse(pomFile)
            assertEquals("Common Java Modules", pom.text("name"))
            val properties = pom.direct("properties").single()
            assertEquals(48, (0 until properties.childNodes.length).count { properties.childNodes.item(it) is Element })
            val fixtureCatalog = File(directory, "gradle/libs.versions.toml")
            assertEquals(fixtureCatalog.fixtureCatalogVersion("spring"), properties.text("spring.version"))
            assertEquals("21", properties.text("java.version"))
            val managed = pom.direct("dependencyManagement").single().direct("dependencies").single().direct("dependency")
            assertTrue(managed.size >= 85)
            fun managedDependency(artifact: String, type: String = "", scope: String = "") =
                managed.single {
                    it.text("artifactId") == artifact && it.text("type") == type && it.text("scope") == scope
                }

            assertEquals("\${project.version}", managedDependency("auth").text("version"))
            assertEquals("\${project.version}", managedDependency("util").text("version"))
            assertEquals("test-jar", managedDependency("auth", "test-jar", "test").text("type"))
            assertEquals("test", managedDependency("auth", "test-jar", "test").text("scope"))
            assertEquals("\${spring.version}", managedDependency("spring-web").text("version"))
            assertEquals("true", managedDependency("spring-web").text("optional"))
            assertEquals("runtime", managedDependency("jersey-media-json-jackson", scope = "runtime").text("scope"))
            assertEquals("provided", managedDependency("lombok", scope = "provided").text("scope"))
            assertEquals("test", managedDependency("spring-test", scope = "test").text("scope"))
            assertEquals("test", managedDependency("test", scope = "test").text("scope"))
            assertEquals(
                "*",
                managedDependency("cxf-jaxws-blueprint", scope = "test")
                    .direct("exclusions").single().direct("exclusion").single().text("artifactId"),
            )
            assertEquals(
                fixtureCatalog.fixtureCatalogVersion("jackson2"),
                managed.single {
                    it.text("groupId") == "com.fasterxml.jackson.core" && it.text("artifactId") == "jackson-databind"
                }.text("version"),
            )
            val build = pom.direct("build").single()
            val managedPlugins = build.direct("pluginManagement").single().direct("plugins").single().direct("plugin")
            val compiler = managedPlugins.single { it.text("artifactId") == "maven-compiler-plugin" }
            assertEquals("3.16.0", compiler.text("version"))
            assertEquals("\${java.version}", compiler.direct("configuration").single().text("target"))
            assertTrue(build.direct("plugins").single().direct("plugin").any { it.text("artifactId") == "flatten-maven-plugin" })
            val profiles = pom.direct("profiles").single().direct("profile")
            assertTrue(profiles.map { it.text("id") }.containsAll(setOf("release", "coverage")))
            assertTrue(
                profiles.single { it.text("id") == "coverage" }
                    .direct("build").single().direct("plugins").single().direct("plugin").single()
                    .direct("executions").single().direct("execution")
                    .any { it.text("id") == "post-test" },
            )
            assertTrue(reusedConfigurationCache)
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
