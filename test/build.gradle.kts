plugins { id("nav.common-library") }

dependencies {
    api(project(":auth"))
    implementation(libs.oauth2.oidc.sdk)
    implementation(libs.commons.io)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
    compileOnly(libs.lombok)
    api(libs.cxf.core)
    runtimeOnly(libs.cxf.ws.security)
    runtimeOnly(libs.istack)
    implementation(libs.cxf.transports.http)
    api(libs.jakarta.servlet)
    testImplementation(libs.hamcrest.core)
    testImplementation(libs.hamcrest.library)
    testImplementation(libs.mockito)
    testImplementation(libs.wiremock)
}
