# Common Java Modules

[![](https://jitpack.io/v/navikt/common-java-modules.svg)](https://jitpack.io/#navikt/common-java-modules)

Felles Java moduler som løser vanlige problemer som f.eks kafka, token exchange, logging, observability etc...

Hver av modulene er laget for å løse et spesifikt behov og man kan plukke og mikse de modulene man trenger etter behov.

## Hvordan ta i bruk

Modulene blir publisert til https://jitpack.io. For at maven/gradle skal hente modulene fra riktig sted så må følgende settes opp.

Maven:
```xml
    <repositories>
        <!-- Legger til central eksplisitt for prioritet over jitpack -->
        <repository>
            <id>central</id>
            <url>https://repo.maven.apache.org/maven2</url>
        </repository>
        <repository>
            <id>jitpack</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
```

Gradle:
```groovy
repositories {
    // Legger til central eksplisitt for prioritet over jitpack
    mavenCentral()
    maven { url "https://jitpack.io" }
}
```

Etter dette er gjort kan man legge til de modulene som man trenger, eksemplet nedenfor viser hvordan man trekker inn **token-client**-modulen:
For å finne siste release versjon av common-java-modules, se https://github.com/navikt/common-java-modules/releases.
Versjonene er på følgende format: `YYYY.MM.DD_HH.mm-SHA`

Maven:
```xml
<dependency>
    <groupId>com.github.navikt.common-java-modules</groupId>
    <artifactId>token-client</artifactId>
    <version>INSERT_LATEST_VERSION</version>
</dependency>
```

Gradle:
```groovy
dependencies {
    implementation 'com.github.navikt.common-java-modules:client:INSERT_LATEST_VERSION'
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
mvn versions:display-dependency-updates
```

## Building the libraries

In order to build `common-java-modules`, run from the root directory:

```shell
mvn clean install
```

### Contact

For questions, create an issue on the Github repository.

See the [CODEOWNERS file](CODEOWNERS) for information on who to contact.
