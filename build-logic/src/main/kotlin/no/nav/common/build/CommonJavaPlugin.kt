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
    "commons-collections4",
    "fastinfoset",
    "istack",
    "jaxb-runtime",
    "jackson2-core",
    "jackson2-databind",
    "jackson2-csv",
    "jackson2-jdk8",
    "kotlin-stdlib",
    "kotlin-reflect",
    "bytebuddy",
)

private fun Project.configureManagedDependencyConstraints() {
    val libs = the<VersionCatalogsExtension>().named("libs")
    MANAGED_CONSTRAINT_ALIASES.forEach { alias ->
        val dependency =
            requireNotNull(libs.findLibrary(alias).orElse(null)) {
                "Mangler libs.$alias for managed constraint"
            }
        dependencies.constraints.add("api", dependency) {
            because("Behold styrte transitive versjoner fra Maven dependencyManagement.")
        }
    }
}
