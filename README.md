# Common Java Modules

[![Build status](https://github.com/navikt/common-java-modules/workflows/Test%20and%20release/badge.svg)](https://github.com/navikt/common-java-modules/workflows/Test%20and%20release/badge.svg)

Felles Java moduler som løser vanlige problemer som f.eks kafka, token exchange, logging, observability etc...

Hver av modulene er laget for å løse et spesifikt behov og man kan plukke og mikse de modulene man trenger etter behov.

## Hvordan ta i bruk

Modulene blir publisert til GitHub Package Registry. For at maven/gradle skal hente modulene fra riktig sted så må følgende settes opp.

Maven:
```xml
    <repositories>
        <!-- Legger til central eksplisitt for prioritet over github -->
        <repository>
            <id>central</id>
            <url>https://repo.maven.apache.org/maven2</url>
        </repository>
        <repository>
            <id>github</id>
            <url>https://github-package-registry-mirror.gc.nav.no/cached/maven-release</url>
        </repository>
    </repositories>
```

Gradle:
```groovy
repositories {
    // Legger til central eksplisitt for prioritet over github
    mavenCentral()
    maven { url "https://github-package-registry-mirror.gc.nav.no/cached/maven-release" }
}
```

Etter dette er gjort kan man legge til de modulene som man trenger, eksemplet nedenfor viser hvordan man trekker inn **token-client**-modulen:
For å finne siste release versjon av common-java-modules, se https://github.com/navikt/common-java-modules/releases.
Versjonene er på følgende format: `4.YYYY.MM.DD_HH.mm-<12-tegns SHA>` (med `-beta` for betaversjoner).

Maven:
```xml
<dependency>
    <groupId>no.nav.common</groupId>
    <artifactId>token-client</artifactId>
    <version>INSERT_LATEST_VERSION</version>
</dependency>
```

Gradle:
```groovy
dependencies {
    implementation 'no.nav.common:token-client:INSERT_LATEST_VERSION'
}
```

## Modules

### Audit log
Audit logging on NAIS. Contains code and logback config to make it easy to write audit logs.

[Read more here](audit-log/README.md)

### Kafka
High-level abstractions built on-top of `org.apache.kafka.kafka-clients` to prevent common mistakes when dealing with kafka.

[Read more here](kafka/README.md)

### Token client
OAuth2 token clients which can be used with Azure AD and TokenX for machine-to-machine and on-behalf-of flows when sending requests between applications. 
Provides easy-to-use clients which requires minimal configuration for applications running on NAIS.

[Read more here](token-client/README.md)

## Version 1
This version is tightly coupled to `api-app` and contains many modules that can be considered deprecated after the release of version 2.
This version will continue to live on the `master` branch until most users of `common-java-modules` has migrated over to version 2.
If possible, no new features should be added to version 1.

## Version 2
Version 2 removes all of the deprecated modules from version 1 (including `api-app`) and aims to provide framework agnostic modules that can be used in any application.

## Checking for new dependency versions

```shell
./gradlew dependencyUpdates
```

## Code formatting

Spotless runs Palantir Java Format for Java and ktlint for Kotlin and Gradle
Kotlin DSL files. Run from the repository root:

```shell
./gradlew spotlessCheck
./gradlew spotlessApply
```

`spotlessCheck` reports formatting differences without changing source files.
`spotlessApply` formats all targeted files, including main/test Java sources,
the Kotlin sources in `build-logic`, and Gradle Kotlin DSL scripts.
Kotlin formatting uses the existing `build-logic/.editorconfig`.
Formatter versions follow the pinned Spotless plugin version.
Java wildcard imports, including static wildcard imports, fail the formatting
check. Use explicit imports. The root `.editorconfig` also configures IntelliJ
to prefer explicit Java imports.

`spotlessCheck` runs automatically as part of `check` and `build`. Formatting
violations fail the build. Run `./gradlew spotlessApply` and commit the formatting
changes before pushing.

## Building the libraries

Use JDK 21 and the checked-in Gradle 9.9.1 wrapper. Docker must be running for
the Kafka/PostgreSQL integration tests. Run from the root directory:

```shell
./gradlew clean build
```

For the local Maven repository equivalent of the old `mvn clean install`,
run `./gradlew clean build publishToMavenLocal`. This is not needed for
normal builds and must not be used when publication is prohibited.

Other useful commands:

```shell
# Build all main/test/source archives and publication POMs, without publishing.
./gradlew publicationArtifacts

# Equivalent of the optional Maven coverage profile.
./gradlew -Pcoverage check jacocoTestReport

# Equivalent of the optional Maven release/Javadoc profile.
./gradlew -Prelease publicationArtifacts
```

Unqualified task names run matching tasks in the root project and all modules.
An explicit path such as `:check` runs only the root task; the root project
has no `:publish` task.

Coordinates remain `no.nav.common:<module>`. The default development version
is `1-SNAPSHOT`; override it with `-Prevision=<version>`. Existing Maven consumer
examples above remain valid.

Gradle is the sole active build. Source-tree Maven POMs have been removed;
Gradle still publishes Maven-compatible module POMs for consumers. New
releases do not include `no.nav.common:mor-pom`; previously published
versions remain available. Maven projects that inherit `mor-pom` or import
it for dependency management must choose another parent or manage versions
directly before moving to a release without it. See
[the migration and validation report](docs/gradle-migration.md) for compatibility
decisions, known limitations, and the rollout/rollback checklist.

## CI follow-up

[Test and release](.github/workflows/main.yml) runs `./gradlew build` for pull
requests and pushes to `main`, including tests and Spotless checks. Only pushes
to `main` can publish a release, after the build succeeds. The
[beta-release workflow](.github/workflows/package-feature-branch.yml) runs tests
when started manually (`workflow_dispatch`), then publishes a beta release.

- [x] Run `./gradlew build` automatically for pull requests, including tests and
  Spotless checks. Restrict the release job to pushes to `main` so pull requests
  cannot publish releases.

## Security follow-up

Track these scanner findings separately from the formatting changes. The alerts
do not establish that sensitive data has been logged or exposed.

- [ ] Review logging and exception messages in `AzureAdMachineToMachineTokenClient`,
  `AzureAdOnBehalfOfTokenClient`, and `TokenXOnBehalfOfTokenClient` in `token-client`.
  Replace complete OAuth error responses with explicitly allowed fields, such as
  the error code. Add regression tests that ensure tokens and sensitive values
  are absent from logs and exception messages. Do not assume that
  `getErrorObject()` returns only safe content.
- [ ] Review `TrustAllX509TrustManager` and `SSLTestUtils` in the `test` module.
  Map their usage, including use outside tests. Prefer test certificates and a
  dedicated truststore over disabled certificate validation. Avoid global TLS
  changes and document any remaining test-only exceptions.

### Contact

For questions, create an issue on the Github repository.

See the [CODEOWNERS file](CODEOWNERS) for information on who to contact.
