import no.nav.common.build.mavenOptional
import org.gradle.api.tasks.testing.Test

plugins { id("nav.common-library") }

publishing.publications.named<MavenPublication>("mavenJava") {
    pom.withXml {
        val repository = asNode().appendNode("repositories").appendNode("repository")
        repository.appendNode("id", "confluent")
        repository.appendNode("url", "https://packages.confluent.io/maven/")
    }
}

tasks.named<Test>("test") {
    // Testcontainers must run against the current Docker environment.
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.org.json)
    api(libs.kafka.clients)
    api(libs.shedlock)
    compileOnly(libs.avro) { mavenOptional() }
    compileOnly(libs.avro.serializer) {
        mavenOptional()
        // Use the explicitly declared Kafka client, not Confluent's differently versioned fork.
        exclude(group = "org.apache.kafka", module = "kafka-clients")
    }
    compileOnly(libs.lombok)
    compileOnly(libs.micrometer) { mavenOptional() }
    compileOnly(libs.spring.jdbc) { mavenOptional() }
    api(project(":json"))
    api(project(":job"))
    api(libs.json.schema) { exclude(group = "commons-logging", module = "commons-logging") }
    testImplementation(project(":log"))
    testImplementation(libs.postgresql)
    testImplementation(libs.hikari)
    testImplementation(libs.h2)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.junit.params)
    testImplementation(libs.mockito)
    testImplementation(libs.awaitility)
}
