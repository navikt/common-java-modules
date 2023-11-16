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