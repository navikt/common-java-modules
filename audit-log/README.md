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

2. Lag en logback fil (logback-naudit.xml) for å konfigurere opp audit loggeren som skal sende data til naudit.
**logback-naudit.xml** kan gjerne ligge i samme mappe som hoved-logback (logback.xml) filen.

Innholdet i naudit logback filen burde se slik ut, husk å bytte ut **MY_APP_NAME** med navnet til applikasjonen.
```xml
<?xml version="1.0" encoding="UTF-8"?>
<included>
    <appender name="AuditLogger" class="com.papertrailapp.logback.Syslog4jAppender">
        <layout class="ch.qos.logback.classic.PatternLayout">
            <pattern>%m%n%xEx</pattern>
        </layout>

        <syslogConfig class="org.productivity.java.syslog4j.impl.net.tcp.TCPNetSyslogConfig">
            <host>audit.nais</host>
            <port>6514</port>
            <ident>MY_APP_NAME</ident>
            <maxMessageLength>128000</maxMessageLength>
        </syslogConfig>
    </appender>

    <logger name="AuditLogger" level="INFO" additivity="false">
        <appender-ref ref="AuditLogger"/>
    </logger>
</included>
```

NB: Hvis du har tatt i bruk **logback-default.xml** fra log-modulen så trekker denne allerede inn audit log configen for deg
og man trenger ikke å gjøre dette steget eller steg 3, men det anbefales å ikke lenger ta i bruk
**logback-default.xml** siden man mister kontrollen på hvilke logback configer som blir trukket inn.

3. Inkluder logback audit loggeren fra hoved-logback filen.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- config for annen logging ... -->
    <include resource="logback-naudit.xml"/>
</configuration>
```

4. Opprett en instanse av audit loggeren og log meldinger

```java
AuditLogger auditLogger = new AuditLoggerImpl();
        
CefMessage cefMessage = CefMessage.builder()
        .applicationName("MY_APP_NAME")
        .event(CefMessageEvent.ACCESS)
        .description("NAV-ansatt har gjort oppslag på bruker")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId("Z12345")
        .destinationUserId("12345678900")
        .timeEnded(System.currentTimeMillis())
        .build();

auditLogger.log(cefMessage);
```

## Ressurser

Informasjon om naudit:
https://github.com/navikt/naudit

Informasjon om CEF formatet:
https://kc.mcafee.com/resources/sites/MCAFEE/content/live/CORP_KNOWLEDGEBASE/78000/KB78712/en_US/CEF_White_Paper_20100722.pdf
