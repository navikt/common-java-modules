plugins { id("nav.common-library") }

dependencies {
    api(project(":util"))
    api(project(":types"))
    api(project(":log"))
    api(project(":rest"))
    api(libs.oauth2.oidc.sdk)
    compileOnly(libs.lombok)
    api(libs.slf4j.api)
    api(libs.jakarta.servlet)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.jersey.hk2)
    testImplementation(libs.jackson.provider)
    testImplementation(libs.jackson.xmlbind)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito)
    testImplementation(libs.wiremock)
    testImplementation(libs.jose4j)
    testImplementation(libs.jetty.annotations)
    testImplementation(libs.jersey.server)
    testImplementation(libs.jersey.container.servlet)
}
