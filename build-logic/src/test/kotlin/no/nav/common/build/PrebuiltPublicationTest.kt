package no.nav.common.build

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

class PrebuiltPublicationTest {
    @Test
    fun `beta publiserer testede POM-er og alle JAR-varianter uten ny kompilering`() {
        val directory = File("build/publication-tests/${UUID.randomUUID()}").absoluteFile.apply { mkdirs() }
        try {
            File(directory, "settings.gradle.kts").writeText(
                """
                rootProject.name = "fixture"
                include("producer", "dependency")
                """.trimIndent(),
            )
            File(directory, "gradle").mkdirs()
            File("../gradle/libs.versions.toml").copyTo(File(directory, "gradle/libs.versions.toml"))
            File(directory, "build.gradle.kts").writeText("""plugins { id("nav.common-root") }""")
            listOf("producer", "dependency").forEach { name ->
                val module = File(directory, name).apply { mkdirs() }
                val dependencies = if (name == "producer") {
                    """
                    dependencies {
                        runtimeOnly(project(":dependency"))
                        constraints { api("example:managed:2.0") }
                    }
                    """.trimIndent()
                } else {
                    ""
                }
                File(module, "build.gradle.kts").writeText(
                    """
                    plugins { id("nav.common-library") }
                    $dependencies
                    """.trimIndent(),
                )
                val source = File(module, "src/main/java/fixture/Marker.java")
                source.parentFile.mkdirs()
                source.writeText("package fixture; public class Marker {}")
            }
            val revision = "4.test-beta"
            val staged = File(directory, "staged")
            GradleRunner
                .create()
                .withProjectDir(directory)
                .withTestKitDir(File("build/test-kit").absoluteFile)
                .withPluginClasspath()
                .withArguments(
                    "publicationArtifacts",
                    "publishAllPublicationsToStagingRepository",
                    "-Prevision=$revision",
                    "-PstagingRepository=$staged",
                    "--configuration-cache",
                ).build()

            val publisher = File(directory, "publisher").apply { mkdirs() }
            listOf("settings.gradle.kts", "build.gradle.kts").forEach { name ->
                File("../scripts/publish-beta/$name").copyTo(File(publisher, name))
            }
            val published = File(directory, "published")
            val runner = GradleRunner
                .create()
                .withProjectDir(publisher)
                .withTestKitDir(File("build/test-kit").absoluteFile)
                .withArguments(
                    "publish",
                    "-Prevision=$revision",
                    "-PstagingRepository=$staged",
                    "-PpublicationRepository=${published.toURI()}",
                    "--configuration-cache",
                    "--offline",
                )
            val result = runner.build()
            assertTrue(runner.build().output.contains("Reusing configuration cache."))
            assertFalse(result.tasks.any { it.path.contains("compile") || it.path.endsWith(":test") || it.path.endsWith(":jar") })
            listOf("producer", "dependency").forEach { module ->
                val path = "no/nav/common/$module/$revision"
                val originals = File(staged, path)
                    .listFiles()
                    .orEmpty()
                    .filter { it.extension in setOf("pom", "jar") }
                assertEquals(5, originals.size)
                originals.forEach { original ->
                    assertArrayEquals(original.readBytes(), File(published, "$path/${original.name}").readBytes(), original.name)
                }
            }
            assertFalse(published.walkTopDown().any { it.extension == "module" })

            File(staged, "no/nav/common/producer/$revision/producer-$revision-tests.jar").delete()
            assertTrue(runner.buildAndFail().output.contains("Mangler ferdigbygd JAR"))
        } finally {
            directory.deleteRecursively()
        }
    }
}
