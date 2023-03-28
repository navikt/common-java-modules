# NAV common kafka

Formålet for denne modulen er å gjøre det lettere å integrere mot Kafka med sane defaults.
Det er også lagt til funksjonalitet for standard feilhåndtering samt forslag til konfigurering av kafka for konsumenter og produsenter.
Modulen er ikke avhengig av rammeverk som f.eks Spring og kan brukes uavhengig.

## Installering

Legg til følgende i pom.xml. Hvis man allerede har avhengigheter som trekker inn kafka (som f.eks spring-kafka), så anbefales det å fjerne disse.
```xml
<dependency>
    <groupId>no.nav.common</groupId>
    <artifactId>kafka</artifactId>
</dependency>
```

## Consumer

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

KafkaConsumerClient<String, String> consumerClient = KafkaConsumerClientBuilder.<String, String>builder()
        .withProps(KafkaPropertiesPreset.aivenDefaultConsumerProperties("<your-app>"))
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

var topicConfigs = List.of(
    new KafkaConsumerClientBuilder.TopicConfig<String, String>()
         .withStoreOnFailure(kafkaConsumerRepository) // Enable store on failure for topic1
         .withConsumerConfig(
                 "topic1",
                 stringDeserializer(),
                 stringDeserializer(),
                 (record) -> { System.out.println(record); }
         )
    new KafkaConsumerClientBuilder.TopicConfig<String, String>()
         .withConsumerConfig(
                 "topic2",
                 stringDeserializer(),
                 stringDeserializer(),
                 (record) -> { System.out.println(record); }
         )
);


KafkaConsumerClient<String, String> consumerClient = KafkaConsumerClientBuilder.<String, String>builder()
                .withProps(KafkaPropertiesPreset.aivenDefaultConsumerProperties("<your-app>"))
                .withTopicConfigs(topicConfigs)
                .build();

consumerClient.start(); // If consumption of records from topic1 fails, then the records will be stored


LockProvider lockProvider = new JdbcTemplateLockProvider(jdbcTemplate);

KafkaConsumerRecordProcessor consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder
                .builder()
                .withLockProvider(lockProvider)
                .withKafkaConsumerRepository(consumerRepository)
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

I flere tilfeller så vil det holde å bruke basic produceren med `sendSync()` hvis man ikke produserer mange meldinger og man tåler at ting feiler når kafka er nede.
NB: Ved bruk av Aiven så vil det oftere bli nedetid siden credentials rulleres og podder har hittil ingen mulighet å få tak i nye uten å bli startet på nytt.

Hvis man ønsker å batche opp meldinger eller ikke ønsker å stoppe opp hvis kafka er nede så kan store-and-forward patternet brukes.
Meldinger vil bli lagret i databasen synkront og vil deretter batches opp og sendes ut gjennom en periodisk job.

For å kunne ta i bruk store-and-forward så må tabellen hvor meldingene skal lagres settes opp.
Eksempler ligger nedenfor:

[Oracle](src/test/resources/kafka-producer-record-oracle.sql)

[Postgres](src/test/resources/kafka-producer-record-postgres.sql)

```java
KafkaProducerRepository producerRepository = new OracleJdbcTemplateProducerRepository(jdbcTemplate);

KafkaProducerRecordStorage<String, String> producerRecordStorage = new KafkaProducerRecordStorage<>(
        producerRepository,
        new StringSerializer(),
        new StringSerializer()
);

producerRecordStorage.store(ProducerUtils.toProducerRecord("topic", "key", "value")); // Store a record in the database
```

I tillegg så trengs det å settes opp en record processor for å publisere de lagrede meldingene.

```java
KafkaProducerClient<byte[], byte[]> producerClient = KafkaProducerClientBuilder.<byte[], byte[]>builder()
           .withProps(KafkaPropertiesPreset.aivenByteProducerProperties("<your-app>"))
           .build();

LeaderElectionClient leaderElectionClient = new LeaderElectionHttpClient();

KafkaProducerRecordProcessor producerRecordProcessor = new KafkaProducerRecordProcessor(producerRepository, producerClient, leaderElectionClient);

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
