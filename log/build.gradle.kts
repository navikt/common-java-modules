plugins { id("nav.common-library") }

dependencies {
    compileOnly(libs.lombok)
    api(project(":util"))
    api(libs.slf4j.api)
    runtimeOnly(libs.jcl.over.slf4j)
    api(libs.logback.classic)
    api(libs.logback.core)
    api(libs.logback.syslog4j)
    api(libs.logstash.logback)
    runtimeOnly(libs.janino)
    runtimeOnly(libs.prometheus.logback)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.mockito)
    testImplementation(libs.assertj)
    testImplementation(libs.jakarta.ws.rs.api)
    testImplementation(libs.gson)
    testImplementation(libs.system.rules)
}
