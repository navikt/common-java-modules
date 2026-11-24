plugins { id("nav.common-library") }

dependencies {
    compileOnly(libs.lombok)
    api(project(":util"))
    api(libs.jackson.databind)
    api(libs.jackson.core)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.annotations)
    runtimeOnly(libs.kotlin.stdlib)
    runtimeOnly(libs.kotlin.reflect)
}
