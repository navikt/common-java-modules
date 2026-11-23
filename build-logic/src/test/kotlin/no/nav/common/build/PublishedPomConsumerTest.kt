package no.nav.common.build

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.io.File
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory

/** Tester avhengigheter slik en ekstern Gradle-konsument leser dem fra publiserte POM-er. */
class PublishedPomConsumerTest {
    @Test
    fun `modulenes POM-er publiserer runtime-versjoner uten å gjøre Avro obligatorisk`() {
        val directory = File("build/publication-tests/${UUID.randomUUID()}").absoluteFile.apply { mkdirs() }
        try {
            val modules = File("..")
                .listFiles()
                .orEmpty()
                .filter { it.name != "build-logic" && File(it, "build.gradle.kts").isFile }
            File(directory, "settings.gradle.kts").writeText(
                """
                rootProject.name = "fixture"
                include(${modules.joinToString { "\"${it.name}\"" }})
                """.trimIndent(),
            )
            File(directory, "gradle").mkdirs()
            val catalog = File("../gradle/libs.versions.toml")
            catalog.copyTo(File(directory, "gradle/libs.versions.toml"))
            File(directory, "build.gradle.kts").writeText("""plugins { id("nav.common-root") }""")
            modules.forEach { module ->
                File(directory, module.name).mkdirs()
                File(module, "build.gradle.kts").copyTo(File(directory, "${module.name}/build.gradle.kts"))
            }

            GradleRunner
                .create()
                .withProjectDir(directory)
                .withPluginClasspath()
                .withArguments("generatePomFileForMavenJavaPublication", "-Prevision=4.test")
                .build()

            val expected = mapOf(
                "audit-log" to mapOf("ch.qos.logback:logback-classic" to "logback", "ch.qos.logback:logback-core" to "logback"),
                "kafka" to mapOf("org.json:json" to "org-json"),
                "health" to mapOf("org.jetbrains.kotlin:kotlin-stdlib" to "kotlin"),
                "json" to mapOf("org.jetbrains.kotlin:kotlin-stdlib" to "kotlin", "org.jetbrains.kotlin:kotlin-reflect" to "kotlin"),
                "cxf" to mapOf("com.sun.istack:istack-commons-runtime" to "istack"),
                "test" to mapOf("com.sun.istack:istack-commons-runtime" to "istack"),
            )
            expected.forEach { (module, artifacts) ->
                val pom = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(File(directory, "$module/build/publications/mavenJava/pom-default.xml"))
                    .documentElement
                val dependencies = (0 until pom.childNodes.length)
                    .mapNotNull { pom.childNodes.item(it) as? Element }
                    .single { it.tagName == "dependencies" }
                val entries = (0 until dependencies.childNodes.length)
                    .mapNotNull { dependencies.childNodes.item(it) as? Element }
                artifacts.forEach { (artifact, versionKey) ->
                    val entry = entries.single {
                        "${it.getElementsByTagName("groupId").item(0).textContent}:" +
                            it.getElementsByTagName("artifactId").item(0).textContent == artifact
                    }
                    assertEquals("runtime", entry.getElementsByTagName("scope").item(0).textContent, "$module:$artifact")
                    assertEquals(
                        catalog.fixtureCatalogVersion(versionKey),
                        entry.getElementsByTagName("version").item(0).textContent,
                        "$module:$artifact",
                    )
                    assertEquals(0, entry.getElementsByTagName("optional").length, "$module:$artifact")
                }
                if (module == "kafka") {
                    listOf("avro", "kafka-avro-serializer").forEach { artifact ->
                        val entry = entries.single { it.getElementsByTagName("artifactId").item(0).textContent == artifact }
                        assertEquals("true", entry.getElementsByTagName("optional").item(0).textContent)
                    }
                    assertFalse(
                        entries.any {
                            it
                                .getElementsByTagName("groupId")
                                .item(0)
                                .textContent
                                .startsWith("com.fasterxml.jackson")
                        },
                    )
                }
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `publisert Java-komponent skiller api fra interne og valgfrie avhengigheter`() {
        // Arrange
        val directory = File("build/publication-tests/${UUID.randomUUID()}").absoluteFile.apply { mkdirs() }
        try {
            File(directory, "settings.gradle.kts").writeText(
                """
                rootProject.name = "fixture"
                include("producer", "apiDependency", "implementationDependency", "runtimeDependency", "optionalDependency", "providedDependency")
                """.trimIndent(),
            )
            File(directory, "gradle").mkdirs()
            File("../gradle/libs.versions.toml").copyTo(File(directory, "gradle/libs.versions.toml"))
            File(directory, "build.gradle.kts").writeText("""plugins { id("nav.common-root") }""")

            listOf(
                "apiDependency",
                "implementationDependency",
                "runtimeDependency",
                "optionalDependency",
                "providedDependency",
            ).forEach { name ->
                module(directory, name)
                val source = File(directory, "$name/src/main/java/fixture/${name}Marker.java")
                source.parentFile.mkdirs()
                source.writeText("package fixture; public class ${name}Marker {}")
            }
            module(
                directory,
                "producer",
                """
                dependencies {
                    api(project(":apiDependency"))
                    implementation(project(":implementationDependency"))
                    runtimeOnly(project(":runtimeDependency"))
                    compileOnly(project(":providedDependency"))
                    compileOnly(project(":optionalDependency")) { mavenOptional() }
                }
                """.trimIndent(),
            )
            val producerSource = File(directory, "producer/src/main/java/fixture/Producer.java")
            producerSource.parentFile.mkdirs()
            producerSource.writeText(
                """
                package fixture;
                public class Producer extends apiDependencyMarker {
                    public String markers() throws ClassNotFoundException {
                        return new implementationDependencyMarker().getClass().getSimpleName()
                            + ":" + Class.forName("fixture.runtimeDependencyMarker").getSimpleName();
                    }
                }
                """.trimIndent(),
            )
            val testSource = File(directory, "producer/src/test/java/fixture/TestData.java")
            testSource.parentFile.mkdirs()
            testSource.writeText("package fixture; public class TestData {}")

            val publisher = GradleRunner
                .create()
                .withProjectDir(directory)
                .withPluginClasspath()
                .withArguments(
                    ":producer:publishMavenJavaPublicationToFixtureRepository",
                    ":apiDependency:publishMavenJavaPublicationToFixtureRepository",
                    ":implementationDependency:publishMavenJavaPublicationToFixtureRepository",
                    ":runtimeDependency:publishMavenJavaPublicationToFixtureRepository",
                    ":optionalDependency:publishMavenJavaPublicationToFixtureRepository",
                    ":providedDependency:publishMavenJavaPublicationToFixtureRepository",
                    "-Prevision=4.test",
                )

            // Act
            publisher.build()

            val consumer = File(directory, "standalone").apply { mkdirs() }
            File(consumer, "settings.gradle.kts").writeText("""rootProject.name = "standalone" """)
            File(consumer, "build.gradle.kts").writeText(
                """
                plugins { `java-library` }
                repositories { maven { url = uri("../build/fixture-repository") } }
                dependencies {
                    implementation("no.nav.common:producer:4.test")
                    testImplementation("no.nav.common:providedDependency:4.test")
                }
                tasks.register<Copy>("copyCompile") {
                    from(configurations["compileClasspath"])
                    into(layout.buildDirectory.dir("compile"))
                }
                tasks.register<Copy>("copyRuntime") {
                    from(configurations["runtimeClasspath"])
                    into(layout.buildDirectory.dir("runtime"))
                }
                tasks.register<Copy>("copyTests") {
                    from(configurations["testRuntimeClasspath"])
                    into(layout.buildDirectory.dir("tests"))
                }
                tasks.register<JavaExec>("runConsumer") {
                    classpath = sourceSets.main.get().runtimeClasspath
                    mainClass.set("fixture.Consumer")
                }
                """.trimIndent(),
            )
            val source = File(consumer, "src/main/java/fixture/Consumer.java")
            source.parentFile.mkdirs()
            source.writeText(
                """
                package fixture;
                public class Consumer extends Producer {
                    public static void main(String[] args) throws ClassNotFoundException {
                        String markers = new Consumer().markers();
                        if (!markers.equals("implementationDependencyMarker:runtimeDependencyMarker")) {
                            throw new AssertionError(markers);
                        }
                    }
                }
                """.trimIndent(),
            )
            GradleRunner
                .create()
                .withProjectDir(consumer)
                .withArguments("compileJava", "runConsumer", "copyCompile", "copyRuntime", "copyTests", "--offline")
                .build()

            fun artifacts(configuration: String): Set<String> = File(consumer, "build/$configuration")
                .listFiles()
                .orEmpty()
                .map { it.name }
                .toSet()

            // Assert
            assertEquals(setOf("producer-4.test.jar", "apiDependency-4.test.jar"), artifacts("compile"))
            assertEquals(
                setOf(
                    "producer-4.test.jar",
                    "apiDependency-4.test.jar",
                    "implementationDependency-4.test.jar",
                    "runtimeDependency-4.test.jar",
                ),
                artifacts("runtime"),
            )
            val tests = artifacts("tests")
            assertEquals(artifacts("runtime") + "providedDependency-4.test.jar", tests)
            val published = File(directory, "build/fixture-repository").walkTopDown().filter { it.isFile }.toList()
            assertTrue(published.any { it.name == "producer-4.test-tests.jar" })
            assertTrue(published.any { it.name == "producer-4.test-test-sources.jar" })
            assertTrue(published.none { it.extension == "module" })

            val pom = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(File(directory, "build/fixture-repository/no/nav/common/producer/4.test/producer-4.test.pom"))
                .documentElement
            val dependencies = (0 until pom.childNodes.length)
                .map { pom.childNodes.item(it) }
                .single { it.nodeName == "dependencies" } as Element

            fun entry(artifact: String): Element = (0 until dependencies.childNodes.length)
                .mapNotNull { dependencies.childNodes.item(it) as? Element }
                .single { it.getElementsByTagName("artifactId").item(0).textContent == artifact }
            assertEquals("true", entry("optionalDependency").getElementsByTagName("optional").item(0).textContent)
            assertEquals("compile", entry("optionalDependency").getElementsByTagName("scope").item(0).textContent)
            assertEquals("provided", entry("providedDependency").getElementsByTagName("scope").item(0).textContent)
            assertEquals("compile", entry("apiDependency").getElementsByTagName("scope").item(0).textContent)
            assertEquals("runtime", entry("implementationDependency").getElementsByTagName("scope").item(0).textContent)
            assertEquals("runtime", entry("runtimeDependency").getElementsByTagName("scope").item(0).textContent)
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun module(
        directory: File,
        name: String,
        dependencies: String = "",
    ) {
        val path = File(directory, name).apply { mkdirs() }
        File(path, "build.gradle.kts").writeText(
            """
            import no.nav.common.build.mavenOptional
            plugins { id("nav.common-library") }
            publishing {
                repositories {
                    maven {
                        name = "Fixture"
                        url = uri(rootProject.layout.buildDirectory.dir("fixture-repository").get().asFile)
                    }
                }
            }
            $dependencies
            """.trimIndent(),
        )
    }
}
