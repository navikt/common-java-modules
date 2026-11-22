plugins { id("nav.common-library") }

dependencies {
    api(project(":util"))
    api(project(":types"))
    api(project(":log"))
    api(project(":rest"))
    api(libs.jakarta.ws.rs.api)
    api(libs.oauth2.oidc.sdk)
    compileOnly(libs.lombok)
    implementation(libs.slf4j.api)
    api(libs.jakarta.servlet)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito)
    testImplementation(libs.wiremock)
    testImplementation(libs.jose4j)
}
