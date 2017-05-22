# API-APP-FEED

Bibliotek for oppsett av feeds over https

### Eksempel på konfigurasjon ved lokal kjøring

````
environment.class=lokalt
feed.lokalt.callback.host=http://localhost:9595/veilarbportefolje/internal/feed/callback
tilordninger.feed.producer.url=http://localhost:8486/veilarbsituasjon/api/feed/
tilordninger.feed.consumer.pollingrate.cron=*/10 * * * * ?
tilordninger.feed.consumer.pollingratewebhook.cron=*/10 * * * * ?
````

Konfigurasjonen gjøres hos konsumenten.

### Installasjon 

Tas i bruk ved å legge til følgende i applikasjonens pom-fil:

```
            <dependency>
                <groupId>no.nav.sbl.dialogarena</groupId>
                <artifactId>feed</artifactId>
                <version>LATEST</version>
            </dependency>
```