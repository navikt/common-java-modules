plugins { id("nav.common-library") }

dependencies {
    api(project(":util"))
    api(project(":log"))
    implementation(libs.org.json)
    implementation(libs.slf4j.api)
    implementation(libs.commons.lang)
    compileOnly(libs.lombok)
    testImplementation(libs.hamcrest.core)
}
