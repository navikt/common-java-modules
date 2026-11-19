# Utfasing av `mor-pom`

Ny publisering av Maven-parenten `no.nav.common:mor-pom` er fjernet fra
Gradle-bygget. De 19 modulene publiseres fortsatt med egne POM-er, uten parent.
Eldre publiserte parent-versjoner skal ikke slettes eller overskrives.

## Før merge og release

Ingen bruk av `mor-pom` ble bekreftet i de undersøkte lokale byggfilene eller
indeksert Nav-kode. Det utelukker ikke private, genererte eller uindekserte
bygg. Avklar med relevante eiere om de arver den som `<parent>`, importerer
den som BOM i `<dependencyManagement>` eller bruker den med Gradle `platform(...)`.

For konsumenter som trenger ny versjon: dokumenter erstatning for arvede
versjoner og Maven-oppsett, la dem teste endringen og avtal varsling før
første release uten parent. En gammel parent-versjon med
`${project.version}` er ikke automatisk kompatibel med en ny modulversjon.

## Kontroller

Kjør fra repo-roten:

```bash
./gradlew :build-logic:test
./gradlew publicationArtifacts
./gradlew publish --dry-run -Prevision=4.test
```

Kontroller at `publicationArtifacts` fortsatt bygger POM-er og arkiver for
alle 19 moduler, at `publish --dry-run` velger alle modulene, og at ingen
`generatePomFileForMorPomPublication` eller `mor-pom`-publisering gjenstår.
Test relevante Maven- og Gradle-konsumenter etter første nye release. En
vellykket dry-run publiserer ikke artefakter og erstatter ikke kontroll av
faktisk publisert release.
