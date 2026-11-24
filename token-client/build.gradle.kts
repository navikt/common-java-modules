plugins { id("nav.common-library") }

dependencies {
    api(libs.oauth2.oidc.sdk)
    compileOnly(libs.lombok)
    api(libs.caffeine)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
    testImplementation(libs.mockwebserver)
}
