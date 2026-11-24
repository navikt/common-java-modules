import no.nav.common.build.mavenOptional

plugins { id("nav.common-library") }

dependencies {
    api(libs.jakarta.servlet)
    api(project(":util"))
    api(libs.okhttp)
    runtimeOnly(libs.kotlin.stdlib)
    compileOnly(libs.lombok)
    compileOnly(libs.micrometer) { mavenOptional() }
    testImplementation(libs.wiremock)
    testImplementation(project(path = ":test", configuration = "testArtifacts"))
}
