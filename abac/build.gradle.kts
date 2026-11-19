import no.nav.common.build.mavenOptional

plugins { id("nav.common-library") }

dependencies {
    api(libs.okhttp)
    api(libs.caffeine)
    api(libs.slf4j.api)
    api(libs.gson)
    compileOnly(libs.lombok)
    api(project(":health"))
    api(project(":types"))
    api(project(":auth"))
    api(project(":util"))
    api(libs.jakarta.servlet)
    compileOnly(libs.spring.web) { mavenOptional() }
    testImplementation(libs.junit.vintage)
    testImplementation(libs.mockito)
    testImplementation(libs.hamcrest.core)
    testImplementation(libs.hamcrest.library)
    testImplementation(libs.wiremock)
    testImplementation(project(":test"))
    testImplementation(project(path = ":test", configuration = "testArtifacts"))
    testImplementation(libs.jose4j)
}
