plugins { id("nav.common-library") }

dependencies {
    api(libs.okhttp)
    compileOnly(libs.lombok)
    api(project(":util"))
    api(project(":log"))
    api(project(":json"))
    api(libs.jakarta.ws.rs.api)
    api(libs.slf4j.api)
    api(libs.jakarta.servlet)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.mockito)
    testImplementation(libs.assertj)
    testImplementation(libs.wiremock)
    testImplementation(libs.spring.test)
    testImplementation(libs.spring.web)
    testImplementation(libs.logback.classic)
}
