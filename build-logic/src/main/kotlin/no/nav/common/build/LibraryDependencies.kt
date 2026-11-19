package no.nav.common.build

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType

/** Holder valgfrie avhengigheter lokale samtidig som bibliotekets testklassebaner bevares. */
internal fun Project.configureLibraryDependencies() {
    val optionalRuntime = configurations.create("optionalRuntime") {
        isCanBeConsumed = false
        isCanBeResolved = false
    }
    configurations.create("optionalRuntimePublication") {
        isCanBeConsumed = false
        isCanBeResolved = false
        extendsFrom(optionalRuntime)
    }

    val compileOnly = configurations.named("compileOnly")
    val runtimeOnly = configurations.named("runtimeOnly")

    configurations.named("runtimeClasspath") {
        extendsFrom(optionalRuntime)
    }
    configurations.named("testImplementation") {
        extendsFrom(compileOnly.get())
    }
    configurations.named("testCompileClasspath") {
        extendsFrom(runtimeOnly.get())
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(Usage.JAVA_RUNTIME))
    }
    compileOnly.configure {
        dependencies
            .withType<ModuleDependency>()
            .configureEach {
                if (isMavenOptional) this@configureLibraryDependencies.dependencies.add(optionalRuntime.name, copy())
            }
    }
}
