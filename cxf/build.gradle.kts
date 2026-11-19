import org.gradle.api.tasks.testing.Test

plugins { id("nav.common-library") }

tasks.named<Test>("test") {
    inputs.dir(layout.projectDirectory.dir("src/main/webapp")).withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    api(project(":auth"))
    api(project(":util"))
    api(libs.cxf.bindings.soap)
    api(libs.cxf.ws.policy)
    api(libs.cxf.frontend.jaxws)
    api(libs.cxf.core)
    api(libs.cxf.frontend.simple)
    api(libs.cxf.ws.security)
    api(libs.cxf.transports.http)
    api(libs.cxf.features.logging)
    api(libs.jsoup) { exclude(group = "commons-logging", module = "commons-logging") }
    api(libs.jakarta.servlet)
    api(libs.slf4j.api)
    compileOnly(libs.lombok)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.mockito)
    testImplementation(libs.hamcrest.core)
    testImplementation(libs.hamcrest.library)
    testImplementation(project(path = ":auth", configuration = "testArtifacts"))
    testImplementation(project(path = ":metrics", configuration = "testArtifacts"))
    testImplementation(libs.commons.io)
    testImplementation(libs.jetty.webapp)
    testImplementation(libs.jetty.server)
    testImplementation(libs.jetty.servlet)
    testImplementation(project(":test"))
    testImplementation(project(path = ":test", configuration = "testArtifacts"))
    testImplementation(libs.assertj)
    testImplementation(libs.logback.core)
    testImplementation(libs.logback.classic)
    testImplementation(project(":rest"))
    testImplementation(libs.spring.test)
    testImplementation(libs.cxf.jaxws.blueprint) { exclude(group = "*", module = "*") }
}
