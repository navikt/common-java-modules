plugins { id("nav.common-library") }

dependencies {
    compileOnly(libs.lombok)
    api(project(":json"))
    implementation(libs.jackson.databind)
    implementation(libs.jackson.yaml)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.engine)
    testImplementation(libs.assertj)
}
