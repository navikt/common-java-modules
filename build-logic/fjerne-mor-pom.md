# Fjerne publiseringen av `mor-pom`

`mor-pom` er en publisert Maven-parent fra før Gradle-migreringen. Fjerning
endrer kontrakten for prosjekter som arver den, importerer den som BOM eller
bruker den med `platform(...)` i Gradle. Dette er ikke en opprydding som kan
gjøres bare fordi de fem testede konsumentrepoene ikke arver den.

## Før fjerning

1. Søk etter `no.nav.common:mor-pom` og `<artifactId>mor-pom</artifactId>` i
   repoer som kan konsumere pakken, også utenfor de fem testede. Se etter
   `<parent>`, BOM-import i `<dependencyManagement>` og Gradle `platform(...)`.
   Kodesøk alene bekrefter ikke at ingen bruker den: avklar også med eiere av
   relevante repoer og eventuelle genererte eller private bygg.
2. Dokumenter hvilke konsumenter som må migreres, og hvordan de skal få
   versjonsstyring og Maven-oppsett uten `mor-pom`. La dem teste migreringen.
   Avtal varsling og tidspunkt for kontraktsbruddet før en release uten parent.
3. Behold gamle publiserte versjoner tilgjengelige. At ny publisering stopper,
   betyr ikke at eksisterende konsumenter kan miste gamle artefakter.

## Endringer i dette repoet

1. Fjern `configureRootPomPublication()` og kallet til den fra
   `CommonRootPlugin` i `BuildConventions.kt`. Behold `configureIdentity()`,
   `base`, `check` og den samlede `publicationArtifacts`-oppgaven. Fjern bare
   avhengigheten til `generatePomFileForMorPomPublication` fra sistnevnte.
   Modulenes publisering og GitHub Packages-oppsett i
   `CommonPublishingConfiguration.kt` og `CommonPublishingPlugin.kt` skal bestå.
2. Fjern `MavenParentCompatibility.kt` når ingen andre referanser til den
   gjenstår. Behold `libraryVersion()`, `COMMON_GROUP` og `DEFAULT_REVISION`
   i `BuildConventions.kt` dersom de fortsatt brukes av modulene.
3. Fjern `RootPomPublicationTest` og oppdater forventningen om rot-POM-oppgaven
   i `NativePublicationTest`. Behold tester av modul-POM-er, test-JAR-er,
   optional/provided/test-scope og konfigurasjonsbufferen.
4. Gå gjennom `gradle/libs.versions.toml` alias for alias. Fjern bare
   oppføringer som ikke lenger brukes av moduler, tester eller andre verktøy;
   `CommonJavaPlugin.kt` sine managed constraints må ikke fjernes automatisk.
5. Oppdater `README.md`, `docs/gradle-migration.md` og eventuell PR-/release-tekst.
   Bevar historiske testresultater som historikk, men ikke beskriv `mor-pom`
   som del av ny publisering. Sjekk workflows som kaller `publish`, uten å
   fjerne modulenes publiseringssteg.

## Kontroller før release

Kjør fra repo-roten:

```bash
./gradlew :build-logic:test
./gradlew publicationArtifacts
./gradlew publish --dry-run -Prevision=4.test
```

Kontroller at `publicationArtifacts` fortsatt bygger modul-POM-er og arkiver,
at `publish --dry-run` velger alle modulene, og at ingen
`generatePomFileForMorPomPublication` eller `mor-pom`-publisering gjenstår.
Test minst én Maven- og én Gradle-konsument som er relevante for den nye
kontrakten. En vellykket dry-run publiserer ikke artefakter og erstatter ikke
en kontroll av faktisk publisert release.
