plugins { id("nav.common-library") }

dependencies {
    api(libs.jackson.annotations)
    testImplementation(project(":json"))
    testImplementation(libs.jackson.databind)
}
