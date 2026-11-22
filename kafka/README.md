# Nav common Kafka

Formålet for denne modulen er å gjøre det lettere å integrere mot Kafka med sane defaults.
Det er også lagt til funksjonalitet for standard feilhåndtering samt forslag til konfigurering av Kafka for konsumenter og produsenter.
Modulen er ikke avhengig av rammeverk som f.eks Spring og kan brukes uavhengig.

## Installering

Legg til følgende i pom.xml. Hvis man allerede har avhengigheter som trekker inn Kafka (som f.eks. spring-kafka), så anbefales det å fjerne disse.
```xml
<dependency>
    <groupId>no.nav.common</groupId>
    <artifactId>kafka</artifactId>
</dependency>
```

## Consumer

### Livssyklus og opprydding

Consumer-klienter og consumer-record-prosessoren kan startes igjen etter `stop()`.
`KafkaConsumerClientImpl.stop()` forsøker å vente på meldingsbehandling, offset-commit og
lukking av Kafka-consumeren. `KafkaConsumerRecordProcessor.stop()` setter bare
stoppflagget og venter ikke på batchen. Ved omstart venter begge på at forrige
kjøring blir ferdig, slik at kjøringene ikke overlapper.
Feature-toggle-wrapperen stopper periodiske toggle-vurderinger før klienten stoppes.
Wrapperens `stop()` forsøker å vente på begge. Feil fra underliggende `stop()` meldes også
når toggle-kjøringen allerede er ferdig.

Bruk consumer-komponentenes `close()` ved endelig lukking, også i testenes `finally` eller teardown.
Metoden venter på callbacks og databasesletting som pågår, og avslutter
alle executorene som komponenten eier. Dette omfatter også metrikkinnhenting og
toggle-vurderinger. Gjentatte `close()`-kall venter på samme lukking uten å starte den
på nytt. `start()` etter `close()` kaster `IllegalStateException`.
Shutdown hooks fjernes når lukking settes i gang, så lenge JVM tillater det.
Consumerens shutdown-hook sender også `wakeup()` før den venter, slik at en langvarig
`poll()` ikke bruker opp ventefristen uten at oppryddingen får starte.

Bibliotekets consumer-implementasjoner venter maksimalt 10 sekunder ved `close()` og
blokkerende `stop()`. Vanlig `stop()` logger tidsavbrudd og avbrutt venting uten
å kaste feil, slik consumer-klienten gjorde tidligere. Retur fra `stop()` bekrefter
derfor ikke at arbeidet er ferdig. Interrupt-statusen gjenopprettes.
Faktiske feil ved lukking av Kafka-consumeren meldes av både vanlig og bekreftet `stop()`.
Dette omfatter også `Error`, som dessuten beholdes for senere bekreftet `close()`.
Bruk `KafkaConsumerClientImpl.stop(Duration)` for bekreftet, restartbar stopp.
Metoden finnes på den konkrete consumer-klienten, ikke på `KafkaConsumerClient`
eller feature-toggle-wrapperen. Grensesnittet utvides ikke med en ny stoppmetode.
De konkrete consumer-klassene har også `close(Duration)` for å velge en annen ventetid.
Ved `stop(Duration)` og `close()` kaster tidsavbrudd og avbrutt venting
`IllegalStateException`. Arbeidet fortsetter i bakgrunnen etter et tidsavbrudd.
Kall `close()` igjen for å bekrefte at alt er avsluttet før databasen ryddes.
Feature-toggle-wrapperen fortsetter å bekrefte underliggende lukking i bakgrunnen når
klientens egen ventefrist utløper. Wrapperens valgte ventefrist gjelder fortsatt kalleren.
Et slikt tidsavbrudd lagres ikke som en permanent lukkefeil; faktiske lukkefeil meldes fortsatt.
Et kall fra komponentens eget arbeid setter stoppflagget eller starter lukking,
men får feil i stedet for å vente på egen avslutning. Dette gjelder også synkron
initialisering og callbacks. Bekreft avslutningen fra en annen tråd.
Feature-toggle-wrapperen avviser callback-venting også ved omstart av en stoppende kjøring,
slik at klienten og callbacken ikke venter på hverandre. Gjentatt `start()` under aktiv kjøring
er fortsatt tillatt og gjør ingenting. En wrapper kan delegere start, stopp og lukking
til sin egen underliggende klient, også når denne er en annen feature-toggle-wrapper.

Bruk `KafkaProducerRecordProcessor.closeAndAwait()` ved endelig, bekreftet lukking
av producer-prosessoren. Metoden venter maksimalt 10 sekunder på publisering,
databasesletting, publisher-lukking og avslutning av executoren.
`closeAndAwait(Duration)` gir en valgt samlet ventetid. Publisheren lukkes nøyaktig
én gang, også før første `start()`, og først etter pågående publisering og
tilhørende databasesletting. Tidsavbrudd, avbrutt venting og lukkefeil meldes som
`IllegalStateException`. Arbeidet fortsetter etter tidsavbrudd; et nytt kall kan
bekrefte avslutningen. Batched og queued publishere beholder eierskapet til produceren.

Producer-prosessorens gamle `close()` beholder ikke-blokkerende oppførsel og er
merket `@Deprecated(forRemoval = true)`. Den ber om endelig lukking, men retur
bekrefter ikke at arbeidet er ferdig. Eksisterende klienter får ikke nye
vente-exceptions fra denne metoden. Migrer til `closeAndAwait()` før metoden fjernes.
Try-with-resources kaller fortsatt gammel `close()` og bekrefter ikke avslutning.
Bruk eksplisitt `closeAndAwait()` i `finally` for bekreftet opprydding.
Spring-beans som trenger bekreftet lukking, må velge
`@Bean(destroyMethod = "closeAndAwait")`; automatisk valg av `close()` venter ikke.
Stoppsekvenser må forsøke øvrig opprydding også når `closeAndAwait()` feiler,
og holde databasen åpen til meldingsbehandlingen som bruker den, er ferdig.

Eksisterende signaturer og konstruktører er beholdt. Standardmetoden
`KafkaConsumerClient.close()` kaller bare `stop()` for tredjepartsimplementasjoner.
Disse må overstyre `close()` for å avvise senere start og frigjøre ressurser som
`stop()` ikke frigjør.
Ved migrering av consumer-tester skal `stop()` i endelig opprydding erstattes med `close()`.
Behold `stop()` der testen undersøker pause, omstart eller offset-endringer.
En callback-teller eller `isRunning() == false` bekrefter ikke at databaseslettingen
er ferdig eller at executorene er avsluttet.

Klienter opprettet med `KafkaConsumerClientBuilder.withToggle()` kan fortsatt
startes igjen etter `stop()`. For Spring-beans kan den nye offentlige `close()`-metoden
bli en [automatisk destroy-callback](https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html#beans-java-lifecycle-callbacks).
Da lukkes klienten endelig når applikasjonskonteksten lukkes. Den kan ikke startes
igjen etterpå. Hvis noen andre enn Spring har ansvar for livssyklusen, kan den
automatiske lukkingen slås av med `@Bean(destroyMethod = "")`. Den ansvarlige må
da kalle `close()` ved endelig lukking.

#### Spring-styrt avslutning

`KafkaConsumerClientBuilder` og `KafkaConsumerRecordProcessorBuilder` har
`withShutdownHookEnabled(boolean)`, slik producer-prosessorens builder allerede har.
Standardverdien er `true`. Eksisterende konstruktører registrerer fortsatt en hook.
Nye konstruktører på `KafkaConsumerClientImpl` og `KafkaConsumerRecordProcessor`
tar også imot valget som en ekstra boolean-parameter. For en feature-toggle-klient
gjelder valget den underliggende consumer-klienten; wrapperen registrerer ikke en egen hook.

Når Spring eier livssyklusen, sett `withShutdownHookEnabled(false)` på hver builder.
Applikasjonen må da sørge for opprydding selv:

- Bruk `SmartLifecycle.stop()` til å kalle consumer-komponentenes `close()` og
  producer-prosessorens `closeAndAwait()` mens databasen fortsatt er tilgjengelig.
- Registrer consumer-ressurser med `@Bean(destroyMethod = "close")` og
  producer-prosessorer med `@Bean(destroyMethod = "closeAndAwait")`. Dette dekker
  også ressurser som aldri ble startet. Gjentatt lukking er tillatt.
- Forsøk øvrig opprydding selv om én komponent melder lukkefeil.

Å deaktivere hooken endrer ikke `start()`, `stop()`, `close()` eller ventefristene.
Det forlenger heller ikke podens avslutningsfrist. Ved tidsavbrudd fortsetter arbeidet
i bakgrunnen; en Spring-stoppfase gir da ikke bekreftelse på fullført lukking.

Ved `ApplicationContext.close()` før JVM-shutdown kan biblioteket fjerne registrerte
hooks. Ved SIGTERM har JVM-shutdown allerede startet: hooks kan kjøre parallelt,
og `Runtime.removeShutdownHook()` kan ikke fjerne dem. Sett derfor valget til `false`
ved opprettelse hvis Spring skal ha eneansvar for avslutningen. Biblioteket forsøker
ikke å oppdage Spring fra classpath.

#### Dokumentasjonsgrunnlag

Kafka-kontraktene er kontrollert mot den låste `kafka-clients`-versjonen **4.4.0**.
Javadoc-sidene nedenfor identifiserer denne versjonen i sidetittelen.

- Consumerens trådeierskap og nye forsøk etter løkkefeil følger
  [KafkaConsumer 4.4.0, «Multi-threaded Processing»](https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#multithreaded).
  Under konsumering brukes bare `wakeup()` fra andre tråder. Poll-tråden venter
  på en latch til consumeren er ferdig initialisert. Ved omstart eller nytt forsøk
  etter en avsluttet løkke opprettes en ny consumer etter at den gamle er lukket.
  `closed`-flagget og `acquireAndEnsureOpen()` er også kontrollert i
  [Apache Kafka-kilden med taggen 4.4.0](https://github.com/apache/kafka/blob/4.4.0/clients/src/main/java/org/apache/kafka/clients/consumer/internals/AsyncKafkaConsumer.java).
- Stoppsignalet og den separate tidsgrensen for venting bygger på
  [wakeup()](<https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#wakeup()>)
  og [poll(Duration)](<https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#poll(java.time.Duration)>).
  Wakeup kan ramme neste blokkerende operasjon,
  og rebalance-callbacks kan vare lenger enn tidsgrensen for poll. Derfor venter
  `stop()` på hele kjøringen. Et wakeup som ikke er håndtert ennå, kan treffe
  siste commit under stopp. Da håndteres `WakeupException`, og commit forsøkes
  igjen med samme offsets.
  Wakeup uten et stoppsignal håndteres som en uventet feil med en ny consumer.
- Meldingsrekkefølgen, ventingen på batcher og lukking etter commit-feil følger
  [seek(TopicPartition, OffsetAndMetadata)](<https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#seek(org.apache.kafka.common.TopicPartition,org.apache.kafka.clients.consumer.OffsetAndMetadata)>),
  [commitSync(Map, Duration)](<https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#commitSync(java.util.Map,java.time.Duration)>)
  og [close(CloseOptions)](<https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#close(org.apache.kafka.clients.consumer.CloseOptions)>).
  Bare fremdrift for ferdigbehandlede meldinger committes. Lukking forsøkes selv
  om commit feiler. Kafka har en egen tidsgrense for lukking av consumerens
  nettverksressurser. Lukking starter først når applikasjonens behandlingstråder er ferdige.
- Kjøringer uten overlapp, publisher-lukking etter publisering og toggle-vurderinger
  bygger på [Java 21 `newSingleThreadExecutor()`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Executors.html#newSingleThreadExecutor())
  og [Java 21 `CountDownLatch`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CountDownLatch.html).
  Hver kjøring har sin egen latch, slik at gamle stoppsignaler ikke påvirker en omstart.
- Toggle-vurderingene beholder fixed-rate-kjøring uten overlapp etter
  [Java 21 `ScheduledExecutorService.scheduleAtFixedRate()`](<https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ScheduledExecutorService.html#scheduleAtFixedRate(java.lang.Runnable,long,long,java.util.concurrent.TimeUnit)>).
  Perioden regnes fra forrige planlagte start, ikke fra slutten av callbacken.
- Lukking av executorer, bekreftet avslutning i tester og eksplisitte tidsavbrudd følger
  Java 21 [shutdown()](<https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#shutdown()>)
  og [awaitTermination()](<https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#awaitTermination(long,java.util.concurrent.TimeUnit)>).
  `shutdownNow()` og executorens [close()](<https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html#close()>) brukes ikke, fordi de kan avbryte
  aktive oppgaver hvis ventingen avbrytes. Bare `true` fra `awaitTermination()`
  bekrefter at executoren er avsluttet. Interrupt-statusen gjenopprettes etter
  [Java 21 `Thread.interrupt()` og `Thread.interrupted()`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html#interrupt()).
  Felles interne ventefunksjoner venter på faktisk fullføring og rapporterer avbrudd
  separat. Kalleren avgjør om kjøringen skal stoppes, og når interrupt-statusen
  skal gjenopprettes. Bakgrunnstråden må vente til initialiseringslatchens `await()`
  returnerer normalt, slik at verdiene fra initialiseringen blir synlige.
- Publisher-eierskapet og ventingen før databasesletting og endelig lukking følger
  [send(ProducerRecord, Callback)](<https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html#send(org.apache.kafka.clients.producer.ProducerRecord,org.apache.kafka.clients.producer.Callback)>),
  [flush()](<https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html#flush()>)
  og [close(Duration)](<https://kafka.apache.org/44/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html#close(java.time.Duration)>)
  fra KafkaProducer 4.4.0.
  `flush()` venter på tidligere sends og callbacks. Producerens `close()` fra en
  send-callback bruker null sekunders ventetid og sikrer ikke at sendte meldinger
  blir bekreftet. Prosessoren lukker derfor publisheren på sin egen executor,
  etter at publisering og databasesletting er ferdig. Ikke kall prosessorens
  blokkerende `closeAndAwait()` fra en send-callback.
- Fjerning av shutdown hooks følger
  [Java 21 `Runtime.removeShutdownHook(Thread)`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Runtime.html#removeShutdownHook(java.lang.Thread)).
  Hooken fjernes ved endelig lukking, unntatt når JVM-avslutningen
  allerede har begynt og registrering ikke lenger kan endres.

### Basic

```java
List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> topicConfigs = List.of(
                new KafkaConsumerClientBuilder.TopicConfig<String, String>()
                        .withConsumerConfig(
                                "topic1",
                                stringDeserializer(),
                                stringDeserializer(),
                                (record) -> { System.out.println(record); }
                        ),
                new KafkaConsumerClientBuilder.TopicConfig<String, String>()
                        .withConsumerConfig(
                                "topic2",
                                stringDeserializer(),
                                stringDeserializer(),
                                 (record) -> { 
                                    somethingThatMightThrowAnException();
                                    return ConsumeStatus.OK;
                                }
                        )
        );

KafkaConsumerClient consumerClient = KafkaConsumerClientBuilder.builder()
        .withProperties(KafkaPropertiesPreset.aivenDefaultConsumerProperties("<your-app>"))
        .withTopicConfigs(topicConfigs)
        .build();

consumerClient.start();

// Records will be consumed from topic1 and topic2
```

Eksempelet ovenfor vil sette opp en klient som konsumerer fra "topic1" og "topic2". 
Hvis en feil opppstår under konsumering av f.eks "topic2", så vil konsumeringen stoppe opp på topicen (kun for partisjonen hvor meldingen ligger)
og konsumering vil bli prøvd på nytt helt til konsumeren returnerer `ConsumeStatus.OK`. 

### Feilhåndtering

Feilhåndtering for konsumere er anbefalt i de tilfellene hvor det kan oppstå feil i koden som konsumerer meldinger 
og man ønsker å lagre den feilende meldingen og fortsette å konsumere slik at ting ikke stopper opp.
Feilhåndtering for consumer settes opp pr topic ved bruk av `StoreOnFailureTopicConsumer`. Dette kan enten gjøres manuelt, eller ved bruk av `KafkaConsumerClientBuilder.TopicConfig`.

For å lagre feilede meldinger så trengs det å legge til nye tabeller ved bruk av migreringsscript.

[Oracle](src/test/resources/kafka-consumer-record-oracle.sql)

[Postgres](src/test/resources/kafka-consumer-record-postgres.sql)

I tillegg til å lagre feilede meldinger, så må også `KafkaConsumerRecordProcessor` brukes for at de lagrede meldingene skal bli rekonsumert.

`KafkaConsumerRecordProcessor` sprer konsumering utover flere instanser av applikasjonen som krever synkronisering mellom instansene ved bruk av shedlock.

Dvs at shedlock må sette opp for applikasjoner som skal bruke `KafkaConsumerRecordProcessor`.

Informasjon om shedlock: https://github.com/lukas-krecan/ShedLock#jdbctemplate

Hvis applikasjonen bruker `JdbcTemplate` så kan **shedlock-provider-jdbc-template** brukes. 
Følgende må gjøres for å sette opp shedlock med JdbcTemplate:

Legg til avhengighet:
```xml
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-provider-jdbc-template</artifactId>
    <version>4.21.0</version>
</dependency>
```

Opprett tabeller i migreringsscript:
```sql
-- Postgres
CREATE TABLE shedlock(name VARCHAR(128) NOT NULL, lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL, locked_by VARCHAR(255) NOT NULL, PRIMARY KEY (name));

-- Oracle
CREATE TABLE shedlock(name VARCHAR(128) NOT NULL, lock_until TIMESTAMP(3) NOT NULL,
    locked_at TIMESTAMP(3) NOT NULL, locked_by VARCHAR(255) NOT NULL, PRIMARY KEY (name));
```

Lag `LockProvider` instanse:
```java
LockProvider lockProvider = new JdbcTemplateLockProvider(/* JdbcTemplate goes here */);
```

Hvordan sette opp topic med feilhåndtering:
```java
JdbcTemplate jdbcTemplate = // Construct from DataSource or inject from bean

KafkaConsumerRepository kafkaConsumerRepository = new PostgresJdbcTemplateConsumerRepository(jdbcTemplate);

List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> topicConfigs = List.of(
    new KafkaConsumerClientBuilder.TopicConfig<String, String>()
         .withStoreOnFailure(kafkaConsumerRepository) // Enable store on failure for topic1
         .withConsumerConfig(
                 "topic1",
                 stringDeserializer(),
                 stringDeserializer(),
                 (record) -> { System.out.println(record); }
         ),
    new KafkaConsumerClientBuilder.TopicConfig<String, String>()
         .withConsumerConfig(
                 "topic2",
                 stringDeserializer(),
                 stringDeserializer(),
                 (record) -> { System.out.println(record); }
         )
);


KafkaConsumerClient consumerClient = KafkaConsumerClientBuilder.builder()
                .withProperties(KafkaPropertiesPreset.aivenDefaultConsumerProperties("<your-app>"))
                .withTopicConfigs(topicConfigs)
                .build();

consumerClient.start(); // If consumption of records from topic1 fails, then the records will be stored


LockProvider lockProvider = new JdbcTemplateLockProvider(jdbcTemplate);

KafkaConsumerRecordProcessor consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder
                .builder()
                .withLockProvider(lockProvider)
                .withKafkaConsumerRepository(kafkaConsumerRepository)
                .withTopicConfigs(topicConfigs)
                .build();

consumerRecordProcessor.start(); // Will periodically consume stored messages
```

#### NB

Hvis en melding feiler, så vil andre meldinger med samme key på samme topic og partisjon ikke bli konsumert. Dette er for at meldinger ikke skal bli konsumert out-of-order.
Det vil derfor potensielt ligge 1 melding som blokker andre meldinger, f.eks. for en gitt bruker dersom man bruker ident som key.

### Metrikker

Det er lagt til støtte for et sett med default prometheus metrikker for consumer/producer.

Metrikker for consumer settes opp pr topic ved bruk av `KafkaConsumerClientBuilder.TopicConfig`:
```java
MeterRegistry registry = /* ... */;

new KafkaConsumerClientBuilder.TopicConfig<String, String>()
         .withMetrics(registry)
         .withLogging()
         .withListener((record, status) -> { /* Can add custom logging or metrics here */})
         .withConsumerConfig(/*...*/)
```

## Producer

### Basic

```java
KafkaProducerClient<String, String> producerClient = KafkaProducerClientBuilder.<String, String>builder()
        .withProps(KafkaPropertiesPreset.aivenDefaultProducerProperties("<your-app>"))
        .build();

// Send synchronously. Will block until sent or throw an exception
producerClient.sendSync(new ProducerRecord<>("topic", "key", "value"));

// Send asynchronously. Will batch up records and send after a short time has passed. Callback is triggered for both failure and success
producerClient.send(new ProducerRecord<>("topic", "key", "value"), ((metadata, exception) -> { /* ... */ }));
```

### Feilhåntering

I flere tilfeller så vil det holde å bruke basic produceren med `sendSync()` hvis man ikke produserer mange meldinger og man tåler at ting feiler når Kafka er nede.
NB: Ved bruk av Aiven så vil det oftere bli nedetid siden credentials rulleres og podder har hittil ingen mulighet å få tak i nye uten å bli startet på nytt.

Hvis man ønsker å batche opp meldinger eller ikke ønsker å stoppe opp hvis Kafka er nede så kan store-and-forward patternet brukes.
Meldinger vil bli lagret i databasen synkront og vil deretter batches opp og sendes ut gjennom en periodisk job.

For å kunne ta i bruk store-and-forward så må tabellen hvor meldingene skal lagres settes opp.
Eksempler ligger nedenfor:

[Oracle](src/test/resources/kafka-producer-record-oracle.sql)

[Postgres](src/test/resources/kafka-producer-record-postgres.sql)

```java
KafkaProducerRepository producerRepository = new OracleJdbcTemplateProducerRepository(jdbcTemplate);

KafkaProducerRecordStorage producerRecordStorage = new KafkaProducerRecordStorage(producerRepository);

ProducerRecord<String, String> record = ProducerUtils.toProducerRecord("topic", "key", "value");

producerRecordStorage.store(ProducerUtils.serializeStringRecord(record)); // Store a record in the database
```

I tillegg så trengs det å settes opp en record processor for å publisere de lagrede meldingene.

```java
KafkaProducerClient<byte[], byte[]> producerClient = KafkaProducerClientBuilder.<byte[], byte[]>builder()
        .withProperties(KafkaPropertiesPreset.aivenByteProducerProperties("<your-app>"))
        .build();

KafkaProducerRecordPublisher recordPublisher = new BatchedKafkaProducerRecordPublisher(producerClient);

LeaderElectionClient leaderElectionClient = new LeaderElectionHttpClient();

KafkaProducerRecordProcessor producerRecordProcessor = KafkaProducerRecordProcessorBuilder.builder()
        .withProducerRepository(producerRepository)
        .withRecordPublisher(recordPublisher)
        .withLeaderElectionClient(leaderElectionClient)
        .build();

producerRecordProcessor.start(); // Will periodically send stored messages
```

### Metrikker

Metrikker for producer kan settes opp gjennom builder:
```java
MeterRegistry registry = /* ... */;

KafkaProducerClient<String, String> producerClient = KafkaProducerClientBuilder.<String, String>builder()
        .withProps(KafkaPropertiesPreset.aivenDefaultConsumerProperties("<your-app>"))
        .withMetrics(registry)
        .build();
```
