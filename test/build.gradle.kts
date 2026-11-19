plugins { id("nav.common-library") }

dependencies {
    api(project(":auth"))
    api(libs.oauth2.oidc.sdk)
    api(libs.commons.io)
    api(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
    compileOnly(libs.lombok)
    api(libs.cxf.core)
    api(libs.cxf.ws.security)
    api(libs.cxf.transports.http)
    api(libs.jakarta.servlet)
    testImplementation(libs.hamcrest.core)
    testImplementation(libs.hamcrest.library)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito)
}
