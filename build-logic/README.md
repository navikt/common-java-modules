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

Begge release-workflowene beregner versjonen før `build` og bygger alle publiseringsartefakter
med samme versjon. `-PstagingRepository=<katalog>` legger til et lokalt Maven-repository
som fylles med `publishAllPublicationsToStagingRepository`. Repositoryet overføres
som en workflow-artefakt først når hele bygget har lykkes.

`scripts/publish-artifacts` er et separat Gradle-bygg uten kompilering eller tester.
Det publiserer de overførte POM- og JAR-filene uendret, inkludert klassifikatorene
`sources`, `tests` og `test-sources`. Testjobben har bare lesetilgang;
publiseringsjobben har skrivetilgang. Testrapporter lastes alltid opp som
`test-reports-<forsøk>`, slik at tidsbruk per test kan undersøkes også etter vellykkede bygg.
Hver modul har et eget publiseringsprosjekt. Workflowene bruker
`--parallel --max-workers=4` for å tillate opptil fire samtidige publiseringer.
Beta-versjonen har suffikset `-beta`, og GitHub-releasen markeres som prerelease.
Ordinære releaser publiseres bare ved push til `main`. Pull requests kjører bygget
uten ekstern publisering.

TestKit bruker `common-java-modules-testkit` under Gradle User Home. Fixture-prosjektene
er fortsatt isolerte, mens avhengigheter og kompilerte byggskript kan gjenbrukes.
Testjobben tar med TestKits `caches`-katalog i CI-cachen. Pull requests leser cache
uten å skrive; manuelle beta-bygg og bygg på `main` kan oppdatere den.

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

Modulene publiserer `mavenJava` fra Java-komponenten. `api` gir `compile`-scope,
mens `implementation` og `runtimeOnly` gir `runtime`-scope i POM-en.
`mavenOptional()` gir en Maven-optional avhengighet
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

### Valg av dependency-scope

| Deklarasjon | Bruk |
|---|---|
| `api` | Avhengighetens typer inngår i offentlige eller beskyttede signaturer, inkludert arv, generiske typer og Lombok-genererte metoder. Eksisterende API-eksporter mellom common-moduler beholdes også av hensyn til konsumentene. |
| `implementation` | Bibliotekets kildekode bruker avhengigheten internt, uten at typene inngår i det offentlige API-et. |
| `runtimeOnly` | Avhengigheten trengs ved kjøring, for eksempel fra XML-konfigurasjon eller refleksjon, men ikke ved kompilering av biblioteket. |
| `compileOnly` | Eksisterende provided-kontrakter og kompilering med Lombok. |
| `compileOnly` med `mavenOptional()` | Valgfrie integrasjoner der konsumenten fortsatt må velge den eksterne avhengigheten selv. |

En intern avhengighet skal ikke flyttes til `runtimeOnly` dersom kildekoden
trenger den ved kompilering. Bruk `implementation` for å skjule slike typer fra
konsumentens compile-klassebane uten å fjerne dem fra runtime.
Eksisterende runtime-avhengigheter beholdes også når ingen kildekode importerer
dem. Å fjerne dem er en egen kontraktsendring, ikke en del av scope-oppryddingen.

Endringer fra `api` til `implementation` eller `runtimeOnly` endrer
konsumentkontrakten. En konsument som importerer den skjulte avhengigheten
direkte, må deklarere den selv. Ikke endre offentlige Java-signaturer for å
skjule flere avhengigheter i samme endring.
Oppryddingen i #686 gjelder derfor tredjepartsavhengigheter, ikke eksisterende
`api(project(...))`-deklarasjoner. Disse eksportene brukes også utenfor
bibliotekenes egne signaturer, blant annet for `JsonUtils`, `MDCConstants` og
`UrlUtils`. Å fjerne dem krever en egen migrering av konsumentene.

Interne `testArtifacts` arver `implementation` (som inkluderer `api`) og
`runtimeOnly`, slik at delte test-JAR-er beholder sine runtime-avhengigheter
når en modul slutter å eksportere dem som API.

## Avhengigheter og versjoner

`../gradle/libs.versions.toml` har koordinater og versjoner, men en katalogoppføring
styrer **ikke** avhengigheter automatisk. `CommonJavaPlugin.kt` velger hvilke aliaser
som blir Gradle-constraints og modulens `dependencyManagement`.

Seks `api`-constraints samordner Logback classic, org.json, istack, Jackson 2
databind, Kotlin stdlib og Kotlin reflect. Jackson 2 samordnes gjennom transitiv
BOM-alignment i Gradle. Byte Buddy har bare en `testImplementation`-constraint
og publiseres ikke som versjonsstyring for konsumenter.

En vanlig Maven-dependency arver ikke bibliotekets `dependencyManagement`.
Derfor deklarerer `audit-log` Logback classic og core direkte, `kafka` deklarerer
org.json, `health` deklarerer Kotlin stdlib, `json` deklarerer Kotlin stdlib og
reflect, og `cxf` og `test` deklarerer istack. Alle publiseres med `runtime`-scope.
Konsumentens egen dependency management kan fortsatt overstyre versjonene.
Avro forblir valgfri; Jackson 2 legges ikke til som en obligatorisk Kafka-avhengighet.
Modul-POM-ene er ikke en felles BOM for valgfrie integrasjoner.

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
