# build-logic

Felles Gradle-konvensjoner for `common-java-modules`, implementert som interne plugins.

## Innhold

- [Kom i gang](#kom-i-gang)
- [Oppbygging](#oppbygging)
- [Hvordan pluginene brukes](#hvordan-pluginene-brukes)
- [Publiseringskontrakt](#publiseringskontrakt)
- [Avhengigheter og versjoner](#avhengigheter-og-versjoner)
- [Viktige filer](#viktige-filer)
- [Endringsguide](#endringsguide)

## Kom i gang

Kjør fra repo-roten med JDK 21:

```bash
./gradlew :build-logic:test
./gradlew :auth:generatePomFileForMavenJavaPublication
```

`publicationArtifacts` bygger arkiver og POM-er for alle 19 moduler uten å
publisere dem.
`publish --dry-run` viser publiseringsoppgaver uten å sende noe til GitHub Packages.

```bash
./gradlew publicationArtifacts
./gradlew publish --dry-run -Prevision=4.test
```

## Oppbygging

Rotens `settings.gradle.kts` kobler inn `build-logic` med `pluginManagement { includeBuild("build-logic") }`.
`build-logic/build.gradle.kts` registrerer plugin-id-ene, mens `build-logic/settings.gradle.kts` leser samme versjonskatalog som hovedbygget.

| Plugin-id | Klasse | Ansvar |
|---|---|---|
| `nav.common-root` | `CommonRootPlugin` | Rotprosjektets identitet, `check` og samlet `publicationArtifacts` |
| `nav.common-java` | `CommonJavaPlugin` | Java 21, test, avhengigheter og rapporter uten publisering |
| `nav.common-publishing` | `CommonPublishingPlugin` | Modul-POM, arkiver, GitHub Packages og Maven-metadata i JAR |
| `nav.common-library` | `CommonLibraryPlugin` | Bruker `nav.common-java` og `nav.common-publishing` sammen |

## Hvordan pluginene brukes

Rotens `build.gradle.kts` bruker `nav.common-root`. Alle bibliotekmodulene bruker
`nav.common-library`, som aktiverer de to spesialiserte pluginene i denne rekkefølgen:

```text
nav.common-library
  -> nav.common-java       (kompilering, tester, klassebaner)
  -> nav.common-publishing (Maven-artefakter og POM)
```

En modul legger sine egne avhengigheter i `build.gradle.kts`, for eksempel
`api(libs.slf4j.api)` eller `compileOnly(libs.micrometer) { mavenOptional() }`.
Ingen modul bruker `nav.common-publishing` direkte.

## Publiseringskontrakt

Modulene publiserer `mavenJava` fra Java-komponenten. `api` og `runtimeOnly`
gir vanlige POM-avhengigheter. `mavenOptional()` gir en Maven-optional avhengighet
uten å eksponere den på Gradle-konsumenters klassebane. `compileOnly` legges til
med `provided`-scope, og testavhengigheter med `test`-scope.
`testsJar` og `testSourcesJar` publiseres med klassifikatorene `tests` og `test-sources`.
En intern `project(..., configuration = "testArtifacts")` peker på samme test-JAR,
men skrives som `test-jar` i en modul-POM. `.module`-metadata er skrudd av slik at
Gradle-konsumenter også leser Maven-POM-en.

Rotprosjektet publiserer ikke lenger `mor-pom`. Modul-POM-ene har ingen parent
og beholder sine egne avhengigheter og constraints. Eldre publiserte
`mor-pom`-versjoner er fortsatt tilgjengelige. Avklar ukjente konsumenter før
en release uten ny parent; se [release-sjekklisten](fjerne-mor-pom.md).

## Avhengigheter og versjoner

`../gradle/libs.versions.toml` har koordinater og versjoner, men en katalogoppføring
styrer **ikke** avhengigheter automatisk. `CommonJavaPlugin.kt` velger hvilke aliaser
som blir Gradle-constraints og modulens `dependencyManagement`.

Ved ny eller endret managed avhengighet: sjekk katalogen og modulens constraints.
Når `withXml` bygger POM-en, må alle verdier allerede være samlet inn ved
konfigurering; lesing av `Project` under oppgavekjøring bryter
konfigurasjonsbufferen.

## Viktige filer

| Fil | Hva den styrer |
|---|---|
| `BuildConventions.kt` | Identitet (`group`/`version`), root-plugin og aggregert `publicationArtifacts`-oppgave |
| `CommonJavaPlugin.kt` | Java- og testkonvensjoner, managed constraints |
| `CommonLibraryConfiguration.kt` | Toolchain, test-artefakter, JUnit/JaCoCo, Mockito/Lombok-oppsett |
| `LibraryDependencies.kt` | `optionalRuntime`, `optionalRuntimePublication` og testklassebaner |
| `CommonPublishingConfiguration.kt` | Maven-publikasjon, optional/provided/test i POM og JAR-metadata |
| `CommonPublishingPlugin.kt` | Aktivering av publisering og metadata-innpakking |
| `MavenOptional.kt` | `mavenOptional()`-markør på avhengigheter |
| `MockitoAgent.kt` | Java-agent-argument til test-tasker |

## Endringsguide

Endrer du klassebaner, se `LibraryDependencies.kt` og `NativeDependenciesTest`.
Endrer du modul-POM eller test-JAR, se `CommonPublishingConfiguration.kt`,
`NativePublicationTest` og `PublishedPomConsumerTest`.
Sjekk også faktiske konsumenter når Maven-kontrakten endres; testene dekker ikke
alle bruksmønstre. Bakgrunn og migreringsgrenser står i
[`docs/gradle-migration.md`](../docs/gradle-migration.md).
