plugins { id("nav.common-library") }

dependencies {
    api(libs.okhttp)
    compileOnly(libs.lombok)
    api(project(":util"))
    api(project(":log"))
    api(project(":json"))
    implementation(libs.slf4j.api)
    api(libs.jakarta.servlet)
    testImplementation(libs.mockito)
    testImplementation(libs.wiremock)
    testImplementation(project(path = ":test", configuration = "testArtifacts"))
    testImplementation(libs.spring.test)
    testImplementation(libs.spring.web)
    testImplementation(libs.logback.classic)
}
