plugins { id("nav.common-library") }

dependencies {
    api(libs.jackson.annotations)
    testImplementation(libs.junit.vintage)
    testImplementation(project(":json"))
    testImplementation(libs.jackson.databind)
}
