plugins { id("nav.common-library") }

dependencies {
    api(libs.oauth2.oidc.sdk)
    compileOnly(libs.lombok)
    api(libs.caffeine)
    api(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
    api(libs.logback.core)
    testImplementation(libs.junit.engine)
    testImplementation(libs.junit.api)
    testImplementation(libs.mockwebserver)
}
