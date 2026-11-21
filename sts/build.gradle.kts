plugins { id("nav.common-library") }

dependencies {
    api(project(":rest"))
    api(project(":auth"))
    api(libs.okhttp)
    api(libs.oauth2.oidc.sdk)
    compileOnly(libs.lombok)
    runtimeOnly(libs.jakarta.servlet)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.mockito)
    testImplementation(libs.assertj)
    testImplementation(libs.wiremock)
}
