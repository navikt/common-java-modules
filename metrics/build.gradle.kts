plugins { id("nav.common-library") }

dependencies {
    api(project(":util"))
    api(project(":log"))
    api(libs.org.json)
    api(libs.slf4j.api)
    api(libs.commons.lang)
    compileOnly(libs.lombok)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.mockito)
    testImplementation(libs.hamcrest.core)
}
