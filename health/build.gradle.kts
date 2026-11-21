import no.nav.common.build.mavenOptional

plugins { id("nav.common-library") }

dependencies {
    api(libs.jakarta.servlet)
    api(project(":util"))
    api(libs.okhttp)
    compileOnly(libs.lombok)
    compileOnly(libs.micrometer) { mavenOptional() }
    testImplementation(libs.junit.vintage)
    testImplementation(libs.wiremock)
}
