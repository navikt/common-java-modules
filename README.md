# Metrics

### [Dokumentasjon i Confluence](http://confluence.adeo.no/pages/viewpage.action?pageId=209466509)
### 👆👆👆👆👆👆👆👆👆👆👆👆👆👆

Hvordan bruke biblioteket og hvordan alt henger sammen.


## Changelog
Endringer / breaking changes

### 3.3.1

**Fluent API** 🎉🎉

```
// Før
Event event = MetricsFactory.createEvent("yo");
event.setFailed();
event.report();

// Nå
MetricsFactory.createEvent("yo").setFailed().report();


// Før
Timer timer = MetricsFactory.createTimer("yo");
timer.start();
// masse kode
timer.stop();
timer.addFieldToReport("field", "value");
timer.report();

// Nå
Timer timer = MetricsFactory.createTimer("yo").start();
// masse kode
timer.stop().addFieldToReport("field", "value").report();
```

Skal ikke være noen knekkende endringer. 

### 3.2.1
Gjør det mulig å rapportere tags ved hjelp av metoden addTagToReport(), ettersom tags blir indeksert av influxDB. 
Dersom du ønsker å kjøre GROUP BY på rapportert data må tags benyttes.


### 3.2.0
Batch-sending av metrikker til Sensu. Kan oppgradere uten endringer, men mulighet for å sette to nye system properties:

    metrics.sensu.report.batchesPerSecond=50
    metrics.sensu.report.batchSize=100
    
Defaultverdiene vist over. Gir throughput på 50x100 meldinger i sekundet. Lagt på, da sensu-client ofte slutter å svare på socket-connections ved høyt press. 
Med 50 batcher i sekunder venter man minst 20ms mellom hver gang, og bør unngå det problemet, samtidig som vi sender flere metrikker i samme kall til sensu.

### 3.1.1
Kan styre feilhåndteringen via system properties.

    metrics.sensu.report.retryInterval=1000
    metrics.sensu.report.queueSize=5000
    
Defaultverdier vist over. Kan oppgradere uten endringer.

### 3.1.0
Håndtering av feilsituasjoner. Spammer ikke loggen i like stor grad. Kan oppgradere uten endringer.

### 3.0.0
Stor omskriving av hele biblioteket for å håndtere to problemer:

* Race condition om flere tråder var innom samme metode med metrikker samtidig. Fungerer nå som det skal.
* Målingene ble sendt i samme tråd som koden den målte, og ved problemer treget det ned koden. Nå gjøres rapporteringen i en egen uavhengig tråd.

APIet er endret minst mulig, så lenge man bare brukte det som var tiltenkt som public API er oppgraderingen rett frem.

### 2.0.1
Midlertidig fix av race condition. Biblioteket må nok skrives stort om.

### 1.7.0
Sender timestamps på millisekundformat i stedet for sekunder. Krav fra AURA, fikser også at metrikker
innenfor samme sekund blir merget.

### 1.6.0
Nedgraderer til Java 1.7 så eldre applikasjoner kan bruke biblioteket

### 1.4.0
Stabilt API

### 1.0.0
POC releaset
