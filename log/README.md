# NAV common log

Denne modulen hjelper deg med å unngå at fødselsnummer logges i ordinær logg. Den tilbyr også oppsett for f.eks. securelogs. 
Modulen fungerer for både springboot-apper og ktor-apper, men krever at du bruker logback. 

## Hvordan bruke modulen

Legg til følgende i pom.xml: 
```xml
<dependency>
    <groupId>no.nav.common</groupId>
    <artifactId>log</artifactId>
    <version>INSERT_LATEST_VERSION</version>
</dependency>
```

eller for gradle:
```groovy
dependencies {
    implementation 'no.nav.common:log:INSERT_LATEST_VERSION'
}
```

I applikasjonen sin logback.xml må du inkludere `logback-stdout-json.xml`, eller du kan forenkle configfilen så den 
passer til ditt bruk. Det viktige er at du bruker appenderen `no.nav.common.log.MaskingAppender`. 

[Her](https://github.com/navikt/amt-tiltaksarrangor-bff/blob/main/src/main/resources/logback-stdout-json.xml) er et eksempel på en litt forenklet configfil.

## Migrering fra Janino

Modulen trekker ikke lenger inn Janino eller `commons-compiler`. CXF-filteret i
`logback-stdout-json.xml` bruker en kompilert `CxfNonErrorEventEvaluator` i stedet
for et Java-uttrykk i XML. Filteret avviser CXF-meldinger på andre nivåer enn
`ERROR` fra ordinær logg. Meldinger på `ERROR` går fortsatt gjennom maskeringen.

Applikasjoner med egne Janino-uttrykk må også migrere konfigurasjonen:

- Erstatt `<evaluator><expression>…</expression></evaluator>` med en kompilert
  `EventEvaluatorBase<ILoggingEvent>`, som beskrevet i
  [Logbacks migreringsguide for evaluatorer](https://logback.qos.ch/manual/filters.html#JaninoEventEvaluator).
- Erstatt `condition`-attributtet på `<if>` med et separat `<condition>`-element.
  Dette krever Logback 1.5.20 eller nyere. Se
  [Logbacks guide for betinget konfigurasjon](https://logback.qos.ch/manual/configuration.html#conditional)
  og [eksemplet i audit-log](../audit-log/README.md#log-output-ved-lokal-kjøring-av-applikasjon).

`logback-cxf.xml` bruker allerede `IsPropertyDefinedCondition` og trenger ingen
endring. `CXF_SECURE_LOG` aktiverer fortsatt CXF-oppsettet når egenskapen er
definert, også når verdien er `false`.