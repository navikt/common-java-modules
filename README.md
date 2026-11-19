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
An explicit path such as `:publish` or `:check` runs only the root task.

Coordinates remain `no.nav.common:<module>`. The default development version
is `1-SNAPSHOT`; override it with `-Prevision=<version>`. Existing Maven consumer
examples above remain valid.

Gradle is the sole active build. Source-tree Maven POMs have been removed;
Gradle still publishes Maven-compatible POMs for consumers. See
[the migration and validation report](docs/gradle-migration.md) for compatibility
decisions, known limitations, and the rollout/rollback checklist.

### Contact

For questions, create an issue on the Github repository.

See the [CODEOWNERS file](CODEOWNERS) for information on who to contact.
