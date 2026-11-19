plugins { id("nav.common-library") }

dependencies {
    compileOnly(libs.lombok)
    api(project(":json"))
    api(libs.jackson.databind)
    api(libs.jackson.yaml)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.engine)
    testImplementation(libs.assertj)
}
