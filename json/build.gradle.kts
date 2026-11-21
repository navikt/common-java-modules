plugins { id("nav.common-library") }

dependencies {
    compileOnly(libs.lombok)
    api(project(":util"))
    api(libs.jackson.databind)
    api(libs.jackson.core)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.annotations)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.engine)
    testImplementation(libs.assertj)
}
