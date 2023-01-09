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
        .extension("msg", "NAV-ansatt har gjort oppslag på bruker")
        .build();

auditLogger.log(cefMessage);
```

4. Når stegene ovenfor er utført og applikasjonen har sendt logger i preprod eller prod, ta kontakt med ArcSight folkene 
i **#auditlogging-arcsight** for å bekrefte at loggene har blitt motatt.

## Log-output ved lokal kjøring av applikasjon
Dersom du kjører opp appen din lokalt og tester endepunkter som har implementert audit-logging så kan du få en feilmelding i konsollen om at du ikke klarer å nå serveren som tar i mot log-statements fra appen din. Det kan derfor være ønskelig å logge lokalt til stdout. Slik kan du gjøre det for en Ktor-applikasjon.

I logback.xml benytter du deg av en if-else og sjekker om miljøvariabelen `NAIS_CLUSTER_NAME` er definert. Hvis den er definert så inkluderer du en `logback-nais.xml`. Dette er logback-filen du bruker i dev og prod til vanlig. Dersom `NAIS_CLUSTER_NAME` ikke er definert inkluderer du en `logback-local.xml` som logger til stdout.
```xml
<configuration>
    <if condition='isDefined("NAIS_CLUSTER_NAME")'>
        <then>
            <include resource="logback-nais.xml"/>
        </then>
        <else>
            <include resource="logback-local.xml"/>
        </else>
    </if>
</configuration>
```

**Eksempel på logback-local.xml**
```xml
<included scan="true" scanPeriod="30 seconds">
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%-5relative %-5level %logger{35} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>

    <logger name="secureLogger" level="INFO" additivity="false">
        <appender-ref ref="STDOUT"/>
    </logger>

    <logger name="org.eclipse.jetty" level="INFO"/>
    <logger name="io.netty" level="INFO"/>
</included>
```

**Legg merke til:** 
Rot-elementet i logback-local.xml som er `<included ...>` og ikke `<configuration>`.

## Ressurser

Informasjon om naudit:
https://github.com/navikt/naudit

Informasjon om CEF formatet:
https://kc.mcafee.com/resources/sites/MCAFEE/content/live/CORP_KNOWLEDGEBASE/78000/KB78712/en_US/CEF_White_Paper_20100722.pdf
