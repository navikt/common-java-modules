# Audit logging

Audit logging på NAV gjøres pr i dag ved å sende CEF-formaterte meldinger til naudit som overfører de videre til ArcSight.

CEF (Common Event Format) er et meldingsformat som brukes for å beskrive en hendelse. Og de ser f.eks slik ut:

```
CEF:0|my-application|AuditLogger|1.0|audit:access|NAV-ansatt har gjort oppslag på bruker|INFO|sproc=0bfe8d02-93d9-4530-99ac-3debae410769 duid=12345678900 end=1654185299215 suid=Z12345
```

## Hvordan ta i bruk audit logging

1. Legg til avhengigheten for audit logging i **pom.xml** filen til applikasjonen:
```xml
<dependency>
    <groupId>no.nav.common</groupId>
    <artifactId>audit-log</artifactId>
    <version>NEWEST_VERSION</version>
</dependency>
```

2. Inkluder logback audit loggeren

NB: Hvis du har tatt i bruk **logback-default.xml** fra log-modulen så trekker denne allerede inn audit log configen for deg
og man trenger ikke å gjøre dette steget, men det anbefales å ikke lenger ta i bruk
**logback-default.xml** siden man mister kontrollen på hvilke logback configer som blir trukket inn.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- config for annen logging ... -->
    <include resource="no/nav/common/audit_log/logback-naudit.xml"/>
</configuration>
```

3. Opprett en instanse av audit loggeren og log meldinger

```java
AuditLogger auditLogger = new AuditLoggerImpl();
        
CefMessage cefMessage = CefMessage.builder()
        .applicationName("MY_APP_NAME")
        .event(CefMessageEvent.ACCESS)
        .name("Sporingslogg")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId("Z12345")
        .destinationUserId("12345678900")
        .timeEnded(System.currentTimeMillis())
        .flexString(1, "reason", "NAV-ansatt har gjort oppslag på bruker")
        .build();

auditLogger.log(cefMessage);
```

4. Når stegene ovenfor er utført og applikasjonen har sendt logger i preprod eller prod, ta kontakt med ArcSight folkene 
i **#tech-logg_analyse_og_datainnsikt** for å bekrefte at loggene har blitt motatt.

## Ressurser

Informasjon om naudit:
https://github.com/navikt/naudit

Informasjon om CEF formatet:
https://kc.mcafee.com/resources/sites/MCAFEE/content/live/CORP_KNOWLEDGEBASE/78000/KB78712/en_US/CEF_White_Paper_20100722.pdf
