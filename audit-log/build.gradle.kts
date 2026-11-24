plugins { id("nav.common-library") }

dependencies {
    compileOnly(libs.lombok)
    runtimeOnly(libs.logback.syslog4j)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.logback.core)
    api(libs.slf4j.api)
}
