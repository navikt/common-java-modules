# Modulbruk og mulige utfasinger

Kartlegging per 19. november 2026, før `abac` ble tatt ut av bygget. Dette er
et utvalg konsumenter, ikke en fullstendig oversikt over Nav-repoer. De 19
øvrige modulene beholdes. Manglende treff alene er ikke grunnlag for å
fjerne flere moduler.

## Undersøkte lokale konsumenter

Datoene er siste commit i lokalt tilgjengelig `origin/main`, ikke datoen da
avhengigheten ble oppdatert. Oppføringene viser bekreftet bruk i lokale
byggfiler, ikke alle modulene hvert repo kan bruke. En lokal `origin/main`
kan ligge etter GitHub.

| Repo | Siste lokale `origin/main`-commit | Bekreftet modulbruk |
|---|---|---|
| [`familie-oppdrag-backend`](https://github.com/navikt/familie-oppdrag-backend/blob/main/pom.xml) | 16.11.2026 | `cxf`, med eldre `com.github.navikt.common-java-modules`-koordinater |
| [`poao-tilgang`](https://github.com/navikt/poao-tilgang) | 05.11.2026 | Blant annet `log`, `token-client`, `rest`, `feature-toggle`, `types` |
| [`kafka-manager`](https://github.com/navikt/kafka-manager/blob/main/api/build.gradle.kts) | 17.11.2026 | `log`, `kafka`, `util`, `rest` |
| [`pto-admin`](https://github.com/navikt/pto-admin/blob/main/api/build.gradle.kts) | 18.11.2026 | `auth`, `client`, `health`, `log`, `token-client`, `types`, `util` |
| [`mulighetsrommet`](https://github.com/navikt/mulighetsrommet) | 30.10.2026 | `audit-log`, `kafka` |
| [`amt-stottetjenester`](https://github.com/navikt/amt-stottetjenester) | 19.11.2026 | Blant annet `audit-log`, `job`, `kafka`, `log`, `rest`, `token-client` |
| [`veilarboppfolging`](https://github.com/navikt/veilarboppfolging/blob/main/build.gradle.kts) | 18.11.2026 | Elleve kjøretidsmoduler og `test` som testavhengighet |
| [`veilarbaktivitet`](https://github.com/navikt/veilarbaktivitet/blob/main/build.gradle.kts) | 17.11.2026 | Elleve kjøretidsmoduler |

I tillegg deklarerer
[`modiapersonoversikt-api`](https://github.com/navikt/modiapersonoversikt-api/blob/main/pom.xml)
`sts` og `nais` med en fellesversjon datert 05.11.2026.
[`veilarbdialog`](https://github.com/navikt/veilarbdialog/blob/main/pom.xml)
deklarerer `nais` med en fellesversjon datert 16.11.2026.
Dette er datoer i versjonsnumrene, ikke verifiserte datoer for siste
commit i disse repoene.

## Vurdering av de opprinnelige 20 modulene

| Vurdering | Moduler | Grunnlag |
|---|---|---|
| Tas ut av ny publisering | `abac` | Den eneste direkte deklarasjonen som er bekreftet her, er i [`tiltak-varsel-ikke-i-bruk`](https://github.com/navikt/tiltak-varsel-ikke-i-bruk/blob/main/pom.xml), låst til en versjon fra mai 2020. Repoet er ifølge bruker ikke oppdatert på fem år. Andre konsumenter er ikke utelukket; gamle publiserte versjoner må forbli tilgjengelige. |
| Ikke slett på grunnlag av få direkte treff | `cxf`, `test`, `sts`, `nais` | Alle har bekreftet bruk. `cxf`-funnet bruker eldre publiseringskoordinater; `test` brukes som testavhengighet. `sts` og `nais` har konsumenter med versjoner fra november 2026. |
| Ikke regn som ubrukte | `json`, `yaml` | Flere moduler eksporterer `json` som intern API-avhengighet. [`nais`](../nais/build.gradle.kts) eksporterer `yaml`, slik at `yaml` også brukes indirekte. |
| Behold ut fra påvist direkte bruk | `audit-log`, `auth`, `client`, `feature-toggle`, `kafka`, `log`, `job`, `metrics`, `rest`, `health`, `types`, `util`, `token-client` | Direkte deklarasjoner er funnet i de undersøkte konsumentene. |

Tilgang til bred GitHub-metadata var begrenset under kartleggingen. Kodesøk
i indekserte filer og de lokale repoene kan verken gi en fullstendig opptelling
av alle Nav-konsumenter eller bevise at en modul ikke brukes. Fjerningen
stopper nye `abac`-utgivelser, ikke bruken av gamle artefakter. Konsumenter
som skal oppdatere til en ny fellesversjon, må avklare hvordan de erstatter
`abac` uten å miste tilgangskontroll.
