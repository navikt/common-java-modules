# Maven to Gradle migration

## Status

Gradle 9.9.1 and Java 21 replaced the source-tree Maven build for 20 modules.
The later removal of `abac` leaves 19 modules in new builds and publications.
Previously published `abac` versions remain available to pinned consumers.
The migration measurements below describe the original 20-module build.
The current build configures Maven-compatible publications for the 19 remaining
modules, but no longer creates or publishes the POM-only `mor-pom`. Previously
published parent versions remain available; their inherited properties and
dependency management do not apply to the standalone module POMs.
No use of `no.nav.common:mor-pom` was confirmed in the searched local builds
or indexed Nav repositories. Private, generated, or unindexed consumers have
not been ruled out. Confirm ownership and notify consumers before releasing
without a new parent version; see [the release checklist](../build-logic/fjerne-mor-pom.md).

The documented final regression beta is **`4.2026.11.19_00.29-1b245c98c320-beta`**,
tagged at `1b245c98c320d5ed34148e3994aa72ac85abb7f5`.

A later beta, **`4.2026.11.19_15.03-d64cef2c94bf-beta`**, was used for the
consumer regression reruns documented below. The latest tested beta is
**`4.2026.11.19_21.37-c369876f3d8f-beta`**.

All five local consumer overrides now point to the 21.37 beta. User builds
passed for `pto-admin`, `veilarbaktivitet`, and `amt-stottetjenester`.
`veilarboppfolging` is not green because embedded PostgreSQL startup failed
in the inspected run; the user reports varying failures across reruns.
`familie-oppdrag-backend` is not green: 125 tests, 1 failure, and 5 errors,
the same counts as before, without a case-by-case comparison.

The previous `4.2026.11.18_22.15-84cd67e68844-beta` had four green Gradle
builds and six unchanged Maven baseline failures in the user's environment.
The earlier 15.03-beta user reruns passed for all four Gradle consumers.
AMT passed with the local test-only readiness change described below. The
15.03-beta Maven user run matched all six known baseline failures; its full
build was not green. These results are historical, not the 21.37-beta outcome.

During the earlier migration verification, the agent did not publish
remotely, run Maven install, create a commit/release/tag, or dispatch a
workflow. Consumer version overrides are local changes in their respective
repositories. `DEPENDENCY-REVIEW.md` remains unchanged.

## Build and publication contract

| Area | Decision |
|---|---|
| Coordinates | Keep `no.nav.common:<module>`. Default version is `1-SNAPSHOT`; `-Prevision=<version>` replaces Maven's `-Drevision=<version>`. |
| Java | Java 21 toolchain and `--release 21`. Lombok remains provided, with explicit main/test annotation processors. |
| Conventions | Included `build-logic` build with `nav.common-root`, `nav.common-java`, `nav.common-library`, and `nav.common-publishing`. |
| Dependencies | Module declarations stay local; versions are in `gradle/libs.versions.toml`. Compile/API, runtime, provided, optional, test, exclusions, and test-JAR semantics are preserved. |
| Archives | Main, `tests`, `sources`, and `test-sources` JARs retain embedded Maven descriptors and implementation/Java metadata. ZIP ordering/timestamps are reproducible, not byte-identical to Maven. |
| Tests | Jupiter and Vintage retain Surefire naming conventions. Mockito uses an explicit test JVM agent. |
| Repositories | Explicit Confluent repository for Kafka; WSS4J's JBoss repository is restricted to OpenSAML/Shibboleth groups. Gradle does not discover repositories from dependency POMs. |

The included build keeps convention tests and Maven compatibility together.
Compatibility code lives in `no.nav.common.build.maven`; module scripts keep
the `no.nav.common.build.mavenOptional` import.
The final beta separated Lombok/Mockito tooling from Maven metadata processing
and simplified parent/module POM generation. No publication contract was removed
in that beta; removing `abac` and then `mor-pom` narrows subsequent releases.

Module POMs have **no parent reference**, explicit effective dependencies,
and only module-specific managed constraints, not the full parent dependency
management. `components.java` supplies ordinary dependencies and optional
variants. The compatibility action adds Maven
`provided`/`test` entries, management, and Kafka's repository. Gradle module
metadata is not published, so consumers resolve the POM contract.

The historical `mor-pom` versions preserved public properties, dependency
management, and Maven build/profile inheritance. Their managed common-module
versions used `${project.version}`: a child with its own version managed common
modules at the child's version, not the parent's release version. The parent
fixtures below deliberately shared the common release version; arbitrary
child versions required overrides. This inheritance is not provided by the
current build or by module POMs.

For the tested final beta, Jackson 3 was **3.1.7**, matching Spring Boot 4.1.2.
The current catalog has Jackson 3 at 3.3.0. Jackson 2 is 2.23.0,
shared annotations are 2.23, and Kotlin stdlib/reflect are 2.4.21.
No Spring Boot BOM was added to the library build.

The migration reduced the root `.gitignore` to 19 rules from 86. Build-output rules remain
anchored so source packages named `build` stay visible. Legacy root/module
`target/` directories and `.flattened-pom.xml` are also ignored. The JSP
exclusion was removed along with `abac`.

Managed-only Jackson 2 and Kotlin artifacts have catalog aliases with
`version.ref`, so dependency-update tooling can associate them with their
version pins. These aliases do not add module dependencies or change versions.

## Dependency scope audit (#686)

The migration preserved dependency scopes. The follow-up for
[#686](https://github.com/navikt/common-java-modules/issues/686) audits all
19 remaining modules and moves 38 existing third-party POM dependencies from
`compile` to `runtime`. All existing API exports between common modules are
retained. Two explicit JAX-RS dependencies preserve `auth`'s public `UriInfo`
type and `client`'s internal HTTP-header imports when `rest` stops exporting
JAX-RS. The number of direct `compile` declarations falls from 108 to 71.
The `types` and `health` modules keep their existing scopes.

Dependencies used by library source code internally use `implementation`,
not `runtimeOnly`. Both publish as Maven `runtime`; only `implementation`
is available when compiling the library itself. Public/protected API types
remain exposed, including inherited types, generic arguments, annotations,
and Lombok-generated signatures. Existing optional/provided contracts,
versions, exclusions, logging bridges, and runtime artifacts are retained.

**Compatibility boundary:** The broader proposal also hid dependencies between
common modules. That candidate caused new compilation failures in `pto-admin`,
`veilarbaktivitet`, `veilarboppfolging`, `kafka-manager`,
`amt-stottetjenester`, and `poao-tilgang`. Missing types included `JsonUtils`,
`JsonMapper`, `MDCConstants`, and `UrlUtils`. Those common-module scope changes
were withdrawn rather than requiring consumer migrations in this change.
No public Java signatures change. The remaining third-party scope changes
still narrow compile classpaths; consumers importing those third-party types
need their own declarations. This is a partial implementation of #686, not a
claim that every internal dependency can now be hidden.

All 19 modules' production and test sources compile. Public/protected bytecode
analysis against exported API dependencies and existing compile-only contracts
finds no unresolved API types. Resolved runtime, test-compile, and test-runtime
coordinates match the pre-audit snapshots for every module. The published-POM
fixture compiles and executes a standalone consumer while keeping internal
implementation/runtime dependencies off its compile classpath.
Internal `testArtifacts` now inherit `implementation` as well as `runtimeOnly`;
the regression fixture covers their retained implementation dependencies.

Production/test compilation succeeds in all 21 repositories from the original
build-file consumer inventory, plus `poao-tilgang` (22 consumers in total):
`pto-admin`, `veilarbaktivitet`, `veilarboppfolging`,
`amt-stottetjenester`, `kafka-manager`, `amt-deltakelser`,
`familie-oppdrag-backend`, `poao-tilgang`, `mulighetsrommet`,
`modiacontextholder`, `modia-robot-api`, `dp-audit-logger`, `dp-stpeter`,
`meldekortservice`, `paw-arbeidssokerregisteret-api`, `please`,
`rekrutteringsbistand-kandidatvarsel-api`,
`rekrutteringsbistand-kandidatsok-api`, `rekrutteringstreff-backend`,
`tilbakekreving`, `toi-rapids-and-rivers`, and `veilarbapi`.
The PAW, rekrutteringstreff, and TOI monorepo runs target the consuming
`schema-registry`, `apps:rekrutteringstreff-api`, and
`apps:toi-synlighetsmotor` modules and their compilation prerequisites.

Both original-scope and conservative-scope POMs pass these compilation targets
with identical library bytecode. This isolates scope compatibility, not every
intervening library change since each consumer's pinned release.
The 20 Gradle consumers retain all 220 resolved runtime/test-runtime graphs
after normalizing only the common candidate version. `meldekortservice`'s
compile-classpath coordinates, which feed its fat JAR, are also unchanged.

Consumer source and build declarations are unchanged. Dependency-lock updates
use isolated copies. Verification-only wiring orders Modia GraphQL generation,
uses package mirrors, and preserves settings-only repository definitions.
The PAW run uses Nav's public package mirror instead of GitHub Packages
credentials. DP compilation-triggered formatting tasks are excluded to avoid
rewriting source files. All eleven newly cloned consumer working trees are
clean after the checks.
These are compilation and dependency-graph checks, not full consumer test-suite
results. Catalog-only references and consumers outside the historical build-file
inventory, apart from `poao-tilgang`, are not covered.

## Unused dependency cleanup

A separate cleanup removes 15 unused test declarations: Mockito from seven
modules, Hamcrest from four, AssertJ from two, JAX-RS from `log`, and the
`test` project dependency from `nais`.

Two redundant runtime declarations are also removed: `logback-core` from
`token-client` and `jackson-core` from `feature-toggle`. Both remain available
transitively through `logback-classic` and `json`, respectively. Production
compile/runtime coordinates are unchanged in all ten affected modules.

Existing common-module API exports, the runtime Logback provider in
`token-client`, and the runtime JAX-RS dependency in `rest` are unchanged.
The consumer results above describe the scope audit; consumer builds were
not rerun for this cleanup.

## Verification

Migration checks used a fresh Maven predecessor and isolated/sequential
builds. Earlier runs with concurrent writes to shared outputs are excluded
as conclusive evidence.

| Check | Result |
|---|---|
| Wrapper | Official Gradle 9.9.1 distribution and wrapper JAR checksums matched. |
| Maven contract | Compared 261 direct declarations and 101 managed entries across 21 POMs; original effective contract preserved before intentional version alignment. Generated POMs also passed offline `help:effective-pom`. |
| Archives/API | Compared 80 archives; class/resource/source entries matched and 397 main classes retained public/protected APIs. Java 21 bytecode verified. |
| Test discovery | All 461 Maven predecessor cases retained their pass/fail/skip status. Gradle adds `JettyPathTest.findsModuleSourcesFromGradleTestClasses`; 15 build-logic tests pass separately. |
| Library suite | Earlier user run after Jackson 3.1.7 alignment: `BUILD SUCCESSFUL in 55s`. Controlled agent runs have seven Kafka Docker-configuration initialization failures and two existing skips. |
| Build-logic refactoring | All 15 convention tests pass. All 21 locally generated POMs and 80 library classpath snapshots were byte-identical before/after the refactoring. |
| Published final beta | All 21 POMs and 80 archives resolve through Nav's package mirror. All 456 published Java source entries match the tagged checkout; embedded Maven identities and class versions are correct. All 80 archive contents match the 22.15 beta except release metadata. |
| Published POM alignment | All 135 Jackson 3 dependency/management entries resolve to 3.1.7. All 21 models match the 22.15 beta after normalizing only the common release version. |
| Consumer graphs | Fresh before/after snapshots of 164 Gradle classpaths in 41 JVM projects and the Maven consumer's selected dependency tree match except for the common release version. |
| Maven contract fixtures | Repeated all 20 test executions against the final beta: direct consumption, `mor-pom`, Boot 4.1.2/4.1.1 BOMs, and parent-property overrides all pass. |
| Cache behavior | Configuration cache stored/reused for artifact generation and dependency snapshots. `publish --dry-run` disables incompatible configuration cache; remote publication cache behavior is not claimed. |

Discovery comparison used module, full class name, method name, and
multiplicity. Only empty trailing `()` in display names were normalized;
failures/errors were grouped as failed outcomes. Test sources are not
identical: build-path/SSL fixture helpers changed, SSL assertions were added,
and the Kafka batch tests now await both topics using thread-safe lists.
No tests were disabled. Coverage XML was produced for 19 modules; Kafka
coverage could not complete in the agent environment.

The Maven fixtures resolve ten common modules through `abac`, `sts`, and
`yaml`. They check interface linkage, JSON maps/lists/date-time values,
stream reads, YAML round-trips, and provided/optional non-export.
Direct/parent/Boot 4.1.2 consumers use Jackson 3.1.7; Boot 4.1.1 and a parent
property override use 3.1.5. They do not test authorization decisions, token
requests, or every API/integration.

## Consumer results

### Latest beta: 21.37

The user ran consumer builds with
`4.2026.11.19_21.37-c369876f3d8f-beta`:

| Consumer | User build result |
|---|---|
| `pto-admin` | `:api:clean :api:build --no-build-cache` succeeded in 14s. |
| `veilarbaktivitet` | `clean build --no-build-cache` succeeded in 1m 57s. |
| `veilarboppfolging` | Repeated builds were not consistently green. The inspected report has 561 tests, 32 failures, and 0 skipped. Embedded PostgreSQL did not start within 10 seconds; subsequent failures in that run could not initialize `LocalDatabaseSingleton`. The user reports that the failures vary between reruns. No test code was changed. |
| `amt-stottetjenester` | `clean build --no-build-cache` succeeded in 2m 8s. |
| `familie-oppdrag-backend` | `mvn clean verify -Dmaven.antrun.skip=true` failed with 125 tests, 1 failure, 5 errors, and 0 skipped. Counts match the previous user run; individual failures were not compared. |

The published POMs were downloaded through Maven, not built or installed
from local source. The `mor-pom` has 108 managed dependency entries,
including `tools.jackson.core:jackson-databind` managed through
`jackson.version=3.1.7`. The `log` module POM has 14 module-specific managed
entries and does not inherit the entire parent management. Maven's dependency
tree for `familie-oppdrag-backend` selects all six common modules at the
21.37 beta. Gradle `dependencyInsight` resolves Jackson databind 3.1.7 on
the inspected runtime classpath in each of the four Gradle consumers.
These checks do not prove full dependency-management parity for every
consumer configuration. `veilarbaktivitet` still selects an older transitive
`audit-log`, and AMT's `amt-person-service` still selects the older transitive
`rest` version documented below; a catalog version does not force alignment
of an entire group.

### Earlier beta: 15.03

All consumer overrides used `4.2026.11.19_15.03-d64cef2c94bf-beta`
for the following earlier checks.

Fresh classpath/tree snapshots were captured with the 22.15 beta before
updating the overrides. No selected dependency changes beyond the common
release version were found. Earlier comparisons included intentional
version updates and were not same-source Maven-versus-Gradle build comparisons.

| Consumer | Final-beta resolution and packaging | Previous user result (22.15 beta) |
|---|---|---|
| `veilarbaktivitet` | Four external classpaths unchanged apart from common beta identity. Executable and distributions packaged. | `BUILD SUCCESSFUL in 2m 2s` |
| `veilarboppfolging` | Four external classpaths unchanged apart from common beta identity. Executable packaged. | `BUILD SUCCESSFUL in 1m 18s` |
| `pto-admin` | Backend compiled/packaged. All four classpaths unchanged apart from common beta identity; the production/test Jackson mismatch remains. | `BUILD SUCCESSFUL in 9s` |
| `amt-stottetjenester` | All 152 external classpaths in 38 JVM projects unchanged apart from common beta identity. Seven applications packaged. | `BUILD SUCCESSFUL in 2m 18s` |
| `familie-oppdrag-backend` | Selected Maven tree unchanged apart from common beta identity. Executable compiled/packaged. | 125 cases: 119 passed, 1 assertion failure, 5 errors, 0 skipped. Same six failing cases/outcomes/types as the preserved beta/main baseline; not a green build. |

The previous user confirmations used `./gradlew clean build --no-build-cache`,
or `./gradlew :api:clean :api:build --no-build-cache` for `pto-admin`.

For the final beta, the user confirmed `veilarbaktivitet`:
`BUILD SUCCESSFUL in 1m 41s`, and `veilarboppfolging`:
`BUILD SUCCESSFUL in 1m 12s`. The `pto-admin` backend also passes:
`BUILD SUCCESSFUL in 8s`. The complete `amt-stottetjenester` build passes:
`BUILD SUCCESSFUL in 1m 48s`. The Maven user run has 125 cases: 119 passed,
1 failure, 5 errors, 0 skipped. Every case outcome and exception type matches
the preserved user baseline; its full build is not green.

After a local machine restart, the user reran the same consumer checks with
`4.2026.11.19_15.03-d64cef2c94bf-beta` and confirmed:
`veilarboppfolging` green, `pto-admin` green, `amt-stottetjenester` green, and
`familie-oppdrag-backend` with one known previously failing test.

Classpath checks were rerun in all five consumers using Gradle
`dependencyInsight`/`dependencies` (and Maven `dependency:tree` for familie).
The selected common modules resolve to the beta in the checked app classpaths.
`amt-person-service` still selects `no.nav.common:rest:4.2026.10.01_11.30-68a5418edc3c`
through `no.nav.poao-tilgang:client`; this was already documented as a
pre-existing transitive alignment limit.

The AMT user rerun failed while `ArrangorIngestorTest` waited for the stored
record (`AssertionError` caused by `NoSuchElementException` at line 43).
Flakiness is suspected, not established. Its test setup now waits for a stable
Kafka group with `amtArrangorTopic` assigned before sending test messages;
the existing ten-second record-processing deadline and assertions are unchanged.
This consumer-test change is separate from the published common beta. It compiles,
but the agent's targeted rerun is blocked by Docker configuration access during
Spring initialization. The user then reported `BUILD SUCCESSFUL in 1m 48s`
and explicitly confirmed that this covers the complete AMT build, not only
the targeted test class.

The final-beta agent runs used `clean build --no-build-cache --rerun-tasks`
with `--continue`, restricted to `:api:clean :api:build` for `pto-admin`.
All 1,963 cases retain their baseline outcomes and exception types:

| Consumer | Cases | Passed | Failures/errors | Skipped |
|---|---:|---:|---:|---:|
| `veilarbaktivitet` | 603 | 218 | 384 | 1 |
| `veilarboppfolging` | 534 | 65 | 469 | 0 |
| `pto-admin` | 29 | 5 | 24 | 0 |
| `amt-stottetjenester` | 695 | 255 | 440 | 0 |
| `familie-oppdrag-backend` | 102 | 58 | 44 | 0 |

These are not green builds. Agent failures involve Docker access, JVM attachment,
PostgreSQL initialization, or already-failed Spring contexts. Maven used fresh
`clean verify` with formatting disabled; separate `package -DskipTests` succeeded
for executable inspection, not as evidence of passing tests. The agent's
102-case Maven run is distinct from the user's previous 125-case result.

### Maven consumer details

The app moved from JitPack
`com.github.navikt.common-java-modules:cxf:4.2026.06.09_07.11-eccd6d131c44`
to canonical `no.nav.common:cxf`. Its `log` exclusion was updated and Nav's
package mirror added. All six common modules (`cxf`, `auth`, `types`, `rest`,
`json`, `util`) use the beta; no JitPack common modules or common `log`
remain. This does not establish compatibility with JitPack's rewritten
coordinates. The app uses Spring Boot's parent, not `mor-pom`.

The matched user baseline is the earlier beta/control comparison against
local `main` commit `91e21eec09491011e0aa9f90a7d0ffc7c660d55f`.
The first send in the double-send integration test returns HTTP 500 instead
of 200; five other errors involve JMS connectivity or missing follow-up
state. Both the 22.15 beta and the final beta retained every case outcome and
exception type in the user's 125-case comparison. The final-beta agent
comparison is documented separately above.
A later 123-case/four-error `main` control run is not the matched baseline.
The failures' root causes are not established by this migration evaluation.

GitHub Packages HTTP 401 initially blocked existing family dependencies
before tests. Session-only settings mirror repository ID `github` to
`https://github-package-registry-mirror.gc.nav.no/cached/maven-release`;
the user completed the beta run through that mirror.
`-Dmaven.antrun.skip=true` prevents the validate-bound ktlint autoformatter
from rewriting sources; it does not skip Surefire tests.

## Compatibility limits

Preserving published POMs does not make development dependency graphs
identical. Historical Maven/Gradle runtime differences affect 11 modules:
`health`, `rest`, `auth`, `test`, `abac`, `client`, `cxf`,
`feature-toggle`, `job`, `kafka`, and `sts`. Examples include Maven's
nearest-first versus Gradle's conflict resolution for Kotlin and Jackson.
The initial comparison predates intentional version alignment.
All eleven now have downstream resolution/compilation coverage, including
`abac`/`sts` fixtures; this neither reconciles those graphs nor covers every API.

A version catalog is not group-wide alignment. `amt-person-service` retains
`no.nav.common:rest:4.2026.10.01_11.30-68a5418edc3c` through
`no.nav.poao-tilgang:client:4.2026.11.01_13.44-d689025f9ad9`.
`veilarbaktivitet` retains an older transitive `audit-log`.
Neither was forced to beta to inflate coverage.

`pto-admin` uses Jackson 3.1.7 in production but 3.2.2 on test-runtime via
`no.nav.security:mock-oauth2-server:6.0.3`. Its green tests do not establish
runtime parity. No forced alignment or authentication changes were added.
The Maven app selects Jackson 3.1.5; representative common JSON/YAML paths
pass on that version, not every possible API path.

### Existing class overlaps

Eleven executable JARs were scanned using effective Java 21/25 multi-release
classes. The final beta retains the complete recorded library/class/collision
models after normalizing the common release version.

| Executables | Existing overlap |
|---|---|
| All eleven | Five Commons Logging classes in `commons-logging` and `jcl-over-slf4j`. |
| Both veilarb apps | An additional 82 Servlet API classes in `jakarta.servlet-api` and embedded Tomcat. |
| `amt-tiltak` | An additional `no.nav.amt.tiltak.utils.UpdateCheck` and `UpdateStatus` in `arrangor`/`tiltak` JARs, with different bytecode. |

Each executable has one SLF4J provider, Logback. All seven amt applications
and the Maven app also retain identical duplicate owners and bytecode.
Initial baseline comparisons found no new duplicate names; these graphs
are not claimed to be collision-free. No exclusions or application logic
were changed to hide overlaps.

## CI and release behavior

`main` pushes trigger ordinary releases; beta releases use manual dispatch.
The test job runs `./gradlew --no-daemon --console=plain build`.
`build` includes `check`, whose root task includes `build-logic:check`.
Dry runs of `check build` and `build` select the same 339 tasks.
Only the release job, gated by `needs: test`, publishes the coordinates
(21 during the original migration, 20 after `abac` removal, and 19 modules
when this change is released).
The registry is `https://maven.pkg.github.com/navikt/common-java-modules`.
All action references are SHA-pinned; Gradle caching uses `setup-gradle`.
Release permissions remain `contents: write` and `packages: write`.
No new credentials were introduced.

Versions remain `4.YYYY.MM.DD_HH.mm-<12-character SHA>`, with `-beta` for
prereleases and Europe/Oslo timestamps. Both release actions now set
`target_commitish: ${{ github.sha }}` to tag the tested/built commit.
The earlier `4.2026.11.18_17.32-50f5c26b6ad8-beta` and
`4.2026.11.18_21.51-4f1f9168f535-beta` tags point to Maven predecessor
`f6f69ab8d9b9687789789ffe673ab3382682a2b6`; they were left unchanged.
Their tag names must not be used as source-provenance evidence.
Ordinary releases do not activate the optional Javadoc profile.

## Reproduction and follow-ups

Use JDK 21 and Docker for the library integration tests. A fresh run without
build-cache reuse, including rerunning tasks in the included build:

```shell
./gradlew clean build --no-build-cache --rerun-tasks
```

With `abac` removed, `org.gradle.parallel=true` enables concurrent module
tasks. `maxParallelForks=1` still limits forks within each test task, not
across modules. Kafka integration tests require a working Docker
configuration.

An earlier agent run of parallel
`build --continue --no-build-cache --rerun-tasks` failed only at `:kafka:test`
(seven tests) because the local Docker configuration could not be read.
The same build with `-x :kafka:test` passed.

- [x] The user confirmed on 21 November 2026 that all tests pass locally,
  including the Kafka tests. The Docker-related follow-up is closed.
  The exact command and parallel-execution details of that run were not recorded.

Artifact generation and release-task inspection without publication:

```shell
./gradlew publicationArtifacts
./gradlew -Prevision=4.2026.11.19_15.03-d64cef2c94bf-beta publish --dry-run
```

Unqualified tasks select the root and matching module tasks; `:build` or
`:check` selects only the root task. The root project no longer has a
`:publish` task. Results are in
`build/test-results/test`, `build/reports/tests/test`, and optional
`build/reports/jacoco/test`. Dependency snapshots are in
`build/reports/dependencies`; update reports are in `build/dependencyUpdates`.

### API Guardian annotations

`warning: unknown enum constant Status.STABLE` came from missing
`org.apiguardian.api.API$Status`. The Maven-compatible test compile
classpath selects `java-runtime` variants to include runtime dependencies;
JUnit's API Guardian annotations are omitted.
The build now adds `org.apiguardian:apiguardian-api:1.1.2` through an isolated
`testCompileAnnotations` configuration inherited only by `testCompileOnly`.
The annotations are not included in runtime configurations or published POMs.

Fresh `compileTestJava` runs for `audit-log`, `json`, `token-client`, and `yaml`
no longer report the API Guardian warnings. The publication regression fixture
checks annotation compilation, runtime isolation, POM omission, and
configuration-cache reuse.

### Javadoc compatibility

The malformed anchors and inline links in `MsGraphClient.java` are fixed,
including escaped ampersands in query strings. The `client:javadoc` task now
completes with doclint enabled. Existing missing-comment and missing-tag
warnings remain; no Java behavior or doclint settings changed.

### TLS test helpers

The `test` module no longer disables certificate or hostname verification.
`SSLTestUtils.createSSLContext(KeyStore)` uses only the supplied truststore.
The no-argument overload trusts only the bundled `dummy.crt` test certificate,
exported from the server fixture `dummy.jks`. Loading client trust requires
neither the fixture's private key nor its keystore password.
`TEST_SSL_SOCKET_FACTORY` also enables HTTPS endpoint identification.
The bundled certificate contains a `localhost` SAN and is a public test fixture,
not a certificate or private key for production use.

The old `TrustAllX509TrustManager`, `TrustAllSSLSocketFactory`,
`TRUST_ALL_SSL_SOCKET_FACTORY`, and `ALLOW_ALL_HOSTNAME_VERIFIER` signatures
remain deprecated adapters. Despite their historical names, they now validate
certificates or hostnames. They do not provide a validation bypass.

**Breaking behavior change:** `disableCertificateChecks()`,
`setupKeyAndTrustStore()`, and `CertificateCheckDisabler.contextInitialized()`
throw `UnsupportedOperationException`. Replace them with TLS settings on the
individual test client. Load a server's test keystore directly rather than
setting JVM-wide `javax.net.ssl.*` properties.
`CxfCertificateValidationDisabler` now configures only the supplied CXF bus or
client with fixture trust and hostname verification. It no longer changes a
default bus or disables SAML validation.

Repository usage was mapped in the published `test` helpers, the
`test/src/main/resources/cxf.xml` feature configuration, and the CXF test
fixtures. `CreateSslConnector` now loads its fixture directly, and the existing
ignored STS integration test uses its own bus. External consumers were not
comprehensively searched. Tests cover trusted and untrusted certificates,
hostname mismatches, the legacy adapters, and unchanged JVM TLS defaults.

### Other review and follow-up items

- Review `pto-admin`'s production/test Jackson mismatch noted above.
- Test jobs still inherit repository/organization token permissions.
  Explicit `contents: read` would avoid those defaults; this is pre-existing.
- Structural cleanup of the Maven compatibility layer was included in the
  final beta. The later removal of new `mor-pom` publications is a separate
  contract change. Removing repeated management or scope rules still requires
  a contract decision and renewed POM/classpath comparisons.

## Privacy and rollback

Existing test fixtures include identifier/token-shaped values; not every
fixture is established as synthetic. Do not upload unreviewed raw logs or
personal data. No production authentication behavior, Nais configuration,
database, or runtime business metrics were introduced by this migration.

Before merge, review the final diff and preserve unrelated user changes.
**A merge to `main` triggers an ordinary release after tests pass.**
Before publication, rollback means reversing only the migration's changes.
After publication, do not overwrite/delete released versions or move tags:
consumers can return to a previous version while a new corrective release
is prepared and separately approved. Consumer beta overrides can be reverted
independently.
