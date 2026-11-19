plugins { id("nav.common-library") }

dependencies {
    compileOnly(libs.lombok)
    api(libs.oauth2.oidc.sdk)
    api(libs.okhttp)
    api(project(":rest"))
    api(project(":health"))
    api(project(":types"))
    api(project(":util"))
    testImplementation(project(path = ":test", configuration = "testArtifacts"))
    api(libs.slf4j.api)
    api(libs.caffeine)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.mockito)
    testImplementation(libs.wiremock)
    testImplementation(libs.jakarta.servlet)
}
