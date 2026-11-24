# Common Java Modules

[![Build status](https://github.com/navikt/common-java-modules/actions/workflows/main.yml/badge.svg)](https://github.com/navikt/common-java-modules/actions/workflows/main.yml)

Felles Java moduler som løser vanlige problemer som f.eks. Kafka, token exchange, logging, observability etc...

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
High-level abstractions built on-top of `org.apache.kafka.kafka-clients` to prevent common mistakes when dealing with Kafka.

[Read more here](kafka/README.md)

### Token client
OAuth2 token clients which can be used with Azure AD and TokenX for machine-to-machine and on-behalf-of flows when sending requests between applications. 
Provides easy-to-use clients which requires minimal configuration for applications running on NAIS.

For TokenX token exchange through Texas in Nais, see
[Texas token exchange](token-client/README.md#exchanging-tokenx-on-behalf-of-token).

[Read more here](token-client/README.md)

## Versions and compatibility

The active development branch is `main`, and current releases use the `4.` prefix.
The build publishes 19 framework-agnostic library modules. Version 1 included
`api-app`; version 2 removed that coupling. Those versions describe historical
APIs, not the current branch layout.

New releases no longer publish `abac` or `mor-pom`. Previously published
versions remain available. See [module usage and retirement](docs/modulbruk-og-utfasing.md)
and [the parent retirement checklist](build-logic/fjerne-mor-pom.md) before
upgrading consumers that depend on either artifact.

The dependency scope cleanup preserves existing API exports between common
modules. Internal third-party dependencies use `implementation` or `runtimeOnly`
where their types are not part of the public API. Runtime dependencies remain
available; dependency versions and optional/provided contracts are unchanged.
See [the dependency scope policy](build-logic/README.md#valg-av-dependency-scope)
for the distinction between API, implementation, runtime, and optional dependencies.

### Removed legacy authentication APIs

New releases no longer include the deprecated `Javax*` authentication helpers in
`auth` and `test`. Use their counterparts without the `Javax` prefix.
The legacy STS classes `AzureAdScopedTokenProvider`, `AzureAdServiceTokenProvider`,
`MsGraphTokenProvider`, `AzureAdServiceTokenProviderBuilder`, and
`sts.utils.AzureAdEnvironmentVariables` have also been removed. Use
[`token-client`](token-client/README.md) for Azure AD tokens and environment constants.
The OpenAM classes `OpenAmSystemUserTokenProvider` and `OpenAmUtils` are removed too.

These removals break source and binary compatibility for callers of those APIs.
Consumers that cannot migrate must pin an older published version.
Existing published versions remain available. Oracle repositories, CXF integration,
and `NaisSystemUserTokenProvider` are retained.

### Removed legacy PDL constructors

The three deprecated `PdlClientImpl` constructors without `behandlingsnummer`
have been removed. Pass the treatment number explicitly to a remaining constructor.
This breaks source and binary compatibility for callers of the removed constructors.
Consumers that cannot migrate must pin an older published version.

### Remaining SSL compatibility cleanup

The `test` module still includes deprecated SSL compatibility APIs. Candidates
for removal are `SSLTestUtils.disableCertificateChecks()`,
`SSLTestUtils.setupKeyAndTrustStore()`, and `CertificateCheckDisabler`, which
reject global TLS changes, plus the `TRUST_ALL_SSL_SOCKET_FACTORY` and
`ALLOW_ALL_HOSTNAME_VERIFIER` aliases in `SSLTestUtils`.

`TrustAllSSLSocketFactory` and `CxfCertificateValidationDisabler` have misleading
names but are still used internally. Replacing them must preserve certificate
and hostname validation, including the CXF XML configuration and tests.
These APIs have not been removed. Future removals will break source and binary
compatibility for their callers.

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

## Dependency follow-up

Unused test declarations and redundant runtime declarations have been removed.
See [the dependency cleanup](docs/gradle-migration.md#unused-dependency-cleanup).
Runtime dependency follow-up:

- [ ] Review the runtime Logback provider in
  [`token-client`](token-client/build.gradle.kts). Move Logback to test scope for
  `TokenErrorResponseTest` only after confirming that consuming applications
  supply their own SLF4J provider.
- [x] Remove the unused runtime `jakarta.ws.rs-api` dependency from
  [`rest`](rest/build.gradle.kts).

**Breaking change for REST consumers:** `rest` no longer supplies
`jakarta.ws.rs:jakarta.ws.rs-api` transitively. Consumers that need JAX-RS must
declare their own dependency. Removal does not wait for consumer migrations.
The dependency remains in common modules that use it, including `auth` and
`client`.

## Test follow-up

The remaining test cleanup is tracked separately from the exception assertions,
Kafka offset/order assertions, secure-log isolation, and instance-bound WireMock
registration.

- [x] Restore `System.out` on failure in `LogbackStdoutJsonTest`. Restore original
  MDC and system property values in the REST, feature-toggle, environment, and
  `InstanceSwitcherTest` fixtures, including expected-exception paths.
- [x] Detach and stop test appenders in `LogRequestFilterTest` and the MDC test in
  `JobRunnerTest`. Close the session returned by `MockitoAnnotations.openMocks()`
  in `LogRequestFilterTest`.
- [x] Close accepted sockets/readers and server sockets in the metrics fixtures.
  Call `SensuHandler.shutdown()` and shut down test executors in
  `SensuHandlerTest` and `SocketIntegrasjonTest`. Assert retry results and replace
  fixed sleeps with bounded waits for observable results.
- [x] Close the producer on failure in `KafkaProducerWithClientMetricsTest` and
  replace its fixed sleep with a bounded wait for completed metric callbacks.
- [x] Make Kafka client/processor cleanup run after failed assertions and waits.
- [x] Separate restartable Kafka `stop()` from terminal, bounded shutdown.
  Final test cleanup waits for active callbacks, publication, database deletion,
  publisher closure and owned executor termination. Consumer processor `stop()`
  remains nonblocking. Restart cannot overlap a draining worker. Plain consumer
  `stop()` logs timeouts and interruption; `stop(Duration)`, consumer `close()`
  and producer `closeAndAwait()` fail explicitly without interrupting active work.
  Legacy producer `close()` remains nonblocking and is deprecated for removal. See the
  [Kafka lifecycle and cleanup contract](kafka/README.md#livssyklus-og-opprydding)
  for compatibility and migration details.
- [x] Await callback counts and database state together in
  `KafkaConsumerRecordProcessorIntegrationTest`; a callback count alone does not
  prove that database deletion has completed.
- [x] In `FeatureToggledKafkaConsumerClientTest`, observe a scheduled poll after
  the feature changes before asserting that `stop()` was not called. Dispose of
  the scheduler after the test.
- [x] Use a thread-safe collection and bounded completion in
  `AuthContextHolderTest`. Propagate interrupted waits instead of ignoring them.
- [x] Change `SystemPropertiesRule` to restore the state captured before each
  evaluation rather than a static class-loading snapshot. Add regression tests
  and assess compatibility for consumers of the published test helper.
- [x] Assert that the filter chain is untouched in the four rejection tests in
  `OidcAuthenticationFilterTest`, in addition to checking HTTP 401.
- [x] Compare the actual serialized JSON body in `RestUtilsTest`, not only its
  length. Cover empty HTTP 200 separately from the duplicated HTTP 204 case.
- [x] Remove the port-probe/bind race in `OidcProviderTestRule` and `PortUtils`.
  Preserve or explicitly migrate helper APIs that expose ports before startup.
- [x] Strengthen `CollectionUtilsTest` partition assertions to cover contents,
  chunk sizes, and ordering. Replace the remaining unchecked list captors with
  typed `ArgumentCaptor.captor()` where supported.

### Test-helper compatibility

`SystemPropertiesRule` now captures system properties immediately before each
evaluation and restores them even when the test fails. `setProperty(name, value)`
retains its signature and fluent return type, but applies the configured values
only during evaluation. Calls from `@Before` or the test body still take effect
immediately and are restored afterward. Code that reads values while constructing
a test must move that work into `@Before` or the test body. Property values are no longer
logged. The rule does not isolate tests that modify system properties concurrently.

`OidcProviderTestRule` now lets WireMock bind an OS-assigned port during startup.
Its constructor and URL getter signatures are unchanged, but URL getters require
a running server. Move URL-dependent initialization into `@Before` or the test
body, after the rule starts. `PortUtils.findAvailablePort()` and its class have
been removed: probing and closing a socket cannot reserve a port for a later
bind. Consumers must use dynamic ports on their server and read the port after
startup. Removing `PortUtils` breaks source and binary compatibility for its
callers in the published `auth` test artifact.

## CI follow-up

[CI](.github/workflows/main.yml) runs `./gradlew build` for pull
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

- [x] Limit OAuth failure output in `AzureAdMachineToMachineTokenClient`,
  `AzureAdOnBehalfOfTokenClient`, and `TokenXOnBehalfOfTokenClient` to a fixed
  operation name, HTTP status, and allowlisted error code. Response descriptions,
  arbitrary error codes, scopes, and parser exception details are not logged or
  included in the resulting exception. Regression tests cover all three clients.
- [x] Replace trust-all behavior in the `test` module with explicit certificate
  trust and hostname verification. Legacy adapters retain their signatures but
  now validate certificates. Global TLS configuration methods fail explicitly
  instead of changing JVM defaults.

**Breaking change for test consumers:** replace calls to
`disableCertificateChecks()`, `setupKeyAndTrustStore()`, and the
`CertificateCheckDisabler` servlet listener with client-local TLS configuration.
See [the TLS test-helper migration](docs/gradle-migration.md#tls-test-helpers).

### Contact

For questions, create an issue on the Github repository.

See the [CODEOWNERS file](CODEOWNERS) for information on who to contact.
