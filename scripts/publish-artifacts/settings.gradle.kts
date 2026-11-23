rootProject.name = "publish-tested-artifacts"

val stagedGroup = file(providers.gradleProperty("stagingRepository").get()).resolve("no/nav/common")
val modules = stagedGroup.listFiles().orEmpty().filter { it.isDirectory }.sortedBy { it.name }
check(modules.isNotEmpty()) { "Fant ingen moduler i $stagedGroup" }

modules.forEach { module ->
    include(module.name)
    project(":${module.name}").projectDir = file("build/modules/${module.name}").apply { mkdirs() }
}
