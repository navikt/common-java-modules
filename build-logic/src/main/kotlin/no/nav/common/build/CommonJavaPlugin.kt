package no.nav.common.build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.the

/** Setter opp Java-, test- og avhengighetskonvensjoner for bibliotekmoduler uten publisering. */
@Suppress("unused")
class CommonJavaPlugin : Plugin<Project> {
    /** Registrerer felles bygge- og testoppsett for bibliotekmoduler. */
    override fun apply(project: Project) {
        project.configureIdentity()

        project.pluginManager.apply("java-library")
        project.pluginManager.apply("jacoco")
        project.pluginManager.apply("io.github.ben-manes.versions")

        project.configureLibraryDependencies()
        project.configureManagedDependencyConstraints()
        project.configureLibraryDependencyTools()
        project.configureJavaToolchain()
        project.configureLibraryRepositories()
        project.configureJavaArtifacts()
        project.configureLibraryTests()
        project.registerDependencySnapshot()
    }
}

private val MANAGED_CONSTRAINT_ALIASES = listOf(
    "logback-classic",
    "org-json",
    "istack",
    "jackson2-databind",
    "kotlin-stdlib",
    "kotlin-reflect",
)

private fun Project.configureManagedDependencyConstraints() {
    val libs = the<VersionCatalogsExtension>().named("libs")
    MANAGED_CONSTRAINT_ALIASES.forEach { alias ->
        val dependency =
            requireNotNull(libs.findLibrary(alias).orElse(null)) {
                "Mangler libs.$alias for managed constraint"
            }
        dependencies.constraints.add("api", dependency) {
            because("Samordne transitive versjoner i Gradle-bygget.")
        }
    }
    dependencies.constraints.add("testImplementation", libs.findLibrary("bytebuddy").get()) {
        because("Samordne Byte Buddy for Mockito og AssertJ uten å styre konsumentenes runtime.")
    }
}
