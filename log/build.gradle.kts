plugins { id("nav.common-library") }

dependencies {
    compileOnly(libs.lombok)
    api(project(":util"))
    api(libs.slf4j.api)
    runtimeOnly(libs.jcl.over.slf4j)
    api(libs.logback.classic)
    api(libs.logback.core)
    runtimeOnly(libs.logback.syslog4j)
    runtimeOnly(libs.logstash.logback)
    runtimeOnly(libs.prometheus.logback)
    testImplementation(libs.gson)
}
