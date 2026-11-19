package no.nav.common.build

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.process.CommandLineArgumentProvider

/**
 * Aktiverer Mockito med en eksplisitt Java-agent, uten dynamisk agenttilkobling.
 *
 * @property agent agent-JAR-en som Gradle sporer som en del av testoppgavens input
 */
class MockitoAgent(
    @get:Classpath val agent: FileCollection,
) : CommandLineArgumentProvider {
    /** Returnerer argumentet med filbanen til Mockito-agenten. */
    override fun asArguments(): Iterable<String> = listOf("-javaagent:${agent.singleFile.absolutePath}")
}
