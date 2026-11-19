package no.nav.common.build

import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.attributes.Usage
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

class NativeDependenciesTest {
    private val directory = File("build/test-projects/${UUID.randomUUID()}").apply { mkdirs() }
    private val project = ProjectBuilder
        .builder()
        .withProjectDir(directory)
        .withGradleUserHomeDir(File("build/test-gradle-home"))
        .build()
        .also { it.pluginManager.apply("java-library") }

    @AfterEach
    fun cleanup() {
        directory.deleteRecursively()
    }

    @Test
    fun `optional er en eksplisitt deklarasjonsmarkør og endrer ikke exclusions`() {
        // Arrange
        val optional = project.dependencies.create("example:optional:1.0") as ModuleDependency
        val regular = project.dependencies.create("example:regular:1.0") as ModuleDependency
        optional.exclude(mapOf("group" to "excluded"))
        val wasOptionalBefore = optional.isMavenOptional

        // Act
        optional.mavenOptional()

        // Assert
        assertFalse(wasOptionalBefore)
        assertTrue(optional.isMavenOptional)
        assertFalse(optional.copy().isMavenOptional)
        assertEquals(optional.excludeRules, optional.copy().excludeRules)
        assertFalse(regular.isMavenOptional)
    }

    @Test
    fun `optional runtime er lokal og compile-only avhengigheter er tilgjengelige i tester`() {
        // Arrange
        project.configureLibraryDependencies()
        val configurations = project.configurations
        val optional = configurations.named("optionalRuntime").get()
        val runtimeClasspath = configurations.named("runtimeClasspath").get()
        val apiElements = configurations.named("apiElements").get()
        val runtimeElements = configurations.named("runtimeElements").get()
        val testImplementation = configurations.named("testImplementation").get()
        val compileOnly = configurations.named("compileOnly").get()
        val testCompileClasspath = configurations.named("testCompileClasspath").get()

        // Act
        val optionalExtendsRuntime = runtimeClasspath.extendsFrom.contains(optional)
        val optionalExtendsApi = apiElements.extendsFrom.contains(optional)
        val optionalExtendsRuntimeElements = runtimeElements.extendsFrom.contains(optional)
        val testImplementationExtendsCompileOnly = testImplementation.extendsFrom.contains(compileOnly)
        val testCompileUsage = testCompileClasspath.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.name

        // Assert
        assertFalse(optional.isCanBeConsumed)
        assertFalse(optional.isCanBeResolved)
        assertTrue(optionalExtendsRuntime)
        assertFalse(optionalExtendsApi)
        assertFalse(optionalExtendsRuntimeElements)
        assertTrue(testImplementationExtendsCompileOnly)
        assertEquals(Usage.JAVA_RUNTIME, testCompileUsage)
    }
}
