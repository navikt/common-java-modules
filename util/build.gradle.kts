plugins { id("nav.common-library") }

dependencies {
    implementation(libs.slf4j.api)
    runtimeOnly(libs.jcl.over.slf4j)
    compileOnly(libs.lombok)
}
