plugins { id("nav.common-library") }

dependencies {
    api(project(":util"))
    api(project(":yaml"))
    implementation(libs.handlebars)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.jcl.over.slf4j)
    compileOnly(libs.lombok)
}
