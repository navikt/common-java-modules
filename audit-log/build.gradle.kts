plugins { id("nav.common-library") }

dependencies {
    compileOnly(libs.lombok)
    api(libs.logback.syslog4j)
    api(libs.slf4j.api)
    testImplementation(libs.junit.engine)
    testImplementation(libs.junit.api)
}
