package no.nav.common.build

import org.gradle.api.Plugin
import org.gradle.api.Project

/** Setter opp felles Java-, test- og publiseringskonvensjoner for bibliotekmoduler. */
@Suppress("unused")
class CommonLibraryPlugin : Plugin<Project> {
    /** Registrerer pluginene, utvidelsene og oppsettsfunksjonene for biblioteket. */
    override fun apply(project: Project) {
        project.pluginManager.apply("nav.common-java")
        project.pluginManager.apply("nav.common-publishing")

        project.tasks.register("publicationArtifacts") {
            group = "build"
            description = "Build this module's publication files and POM without publishing anything."
            dependsOn("assemble", "testsJar", "testSourcesJar", "generatePomFileForMavenJavaPublication")
        }
    }
}
