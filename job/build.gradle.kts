plugins { id("nav.common-library") }

dependencies {
    api(project(":json"))
    api(project(":log"))
    api(project(":util"))
    implementation(libs.okhttp)
    compileOnly(libs.lombok)
    api(libs.shedlock)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.wiremock)
    testImplementation(project(":test"))
    testImplementation(project(path = ":test", configuration = "testArtifacts"))
}
