plugins { id("nav.common-library") }

dependencies {
    api(libs.unleash)
    api(project(":util"))
    api(project(":auth"))
    api(project(":health"))
    api(project(":types"))
    api(project(":json"))
    api(project(":rest"))
    runtimeOnly(libs.jackson.core)
    runtimeOnly(libs.jakarta.ws.rs.api)
    implementation(libs.slf4j.api)
    compileOnly(libs.lombok)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.wiremock)
    testImplementation(libs.mockito)
    testImplementation(libs.assertj)
    testImplementation(libs.okhttp)
    testImplementation(libs.mockwebserver)
}
