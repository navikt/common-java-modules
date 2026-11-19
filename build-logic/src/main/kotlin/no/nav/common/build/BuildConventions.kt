package no.nav.common.build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
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

/** Konfigurerer rotprosjektets oppgaver. */
@Suppress("unused")
class CommonRootPlugin : Plugin<Project> {
    /** Registrerer rotprosjektets egne oppgaver uten å konfigurere modulene. */
    override fun apply(project: Project): Unit = with(project) {
        configureIdentity()
        pluginManager.apply("base")

        tasks.named("check") {
            // Et inkludert bygg omfattes ikke automatisk av rotprosjektets check.
            dependsOn(gradle.includedBuild("build-logic").task(":check"))
        }

        tasks.register("publicationArtifacts") {
            group = "build"
            description = "Build all publication artifacts without publishing anything."
            dependsOn(subprojects.map { "${it.path}:publicationArtifacts" })
        }
    }
}
