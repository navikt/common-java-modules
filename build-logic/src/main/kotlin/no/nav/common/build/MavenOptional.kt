package no.nav.common.build

import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.plugins.ExtensionAware

private const val MAVEN_OPTIONAL_KEY = "no.nav.common.maven.optional"

/** Beholder Maven optional uten å eksponere avhengigheten til Gradle-konsumenter. */
fun ModuleDependency.mavenOptional() {
    val extensionAware = this as? ExtensionAware ?: error("ModuleDependency must be ExtensionAware")
    extensionAware.extensions.extraProperties.set(MAVEN_OPTIONAL_KEY, true)
}

internal val ModuleDependency.isMavenOptional: Boolean
    get() {
        val extensionAware = this as? ExtensionAware ?: return false
        val extraProperties = extensionAware.extensions.extraProperties
        return extraProperties.has(MAVEN_OPTIONAL_KEY) &&
            extraProperties.get(MAVEN_OPTIONAL_KEY) == true
    }
