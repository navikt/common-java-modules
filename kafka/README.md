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
Credentials credentials = new Credentials("username", "password");

KafkaConsumerClient<String, String> consumerClient = KafkaConsumerClientBuilder.<String, String>builder()
        .withProps(KafkaProperties.defaultConsumerProperties("group_id", "broker_url", credentials))
        .withTopic("topic1", (record) -> {
            System.out.println("Record from topic 1: " + record.value());
            return ConsumeStatus.OK;
        })
        .withTopic("topic2", (record) -> {
            System.out.println("Record from topic 2: " + record.value());
            somethingThatMightThrowAnException();
            return ConsumeStatus.OK;
        })
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
Feilhåndtering for consumer settes opp pr topic ved bruk av `StoreOnFailureTopicConsumer`. Dette kan enten gjøres manuelt, eller ved bruk av `KafkaConsumerClientBuilder`.

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
DataSource dataSource = /* Must be retrieved from somewhere, f.eks JdbcTemplate.getDataSource() */;

Credentials credentials = new Credentials("username", "password");

KafkaConsumerRepository kafkaConsumerRepository = new OracleConsumerRepository(dataSource);

TopicConsumer<String, String> topic1Consumer = new JsonTopicConsumer<>(KafkaMessageDTO.class, (dto) -> ConsumeStatus.OK);
TopicConsumer<String, String> topic2Consumer = new JsonTopicConsumer<>(KafkaMessageDTO.class, (dto) -> ConsumeStatus.OK);

Map<String, TopicConsumer<String, String>> consumers = Map.of(
        "topic1", topic1Consumer,
        "topic2", topic2Consumer
);

KafkaConsumerClient<String, String> consumerClient = KafkaConsumerClientBuilder.<String, String>builder()
                .withProps(KafkaProperties.onPremDefaultConsumerProperties("group_id", "broker_url", credentials))
                .withRepository(kafkaConsumerRepository) // Required for storing records
                .withSerializers(new StringSerializer(), new StringSerializer()) // Required for serializing the record into byte[]
                .withStoreOnFailureConsumers(consumers) // Enable store on failure for topics
                .build();

consumerClient.start(); // Records will be stored in database if the consumer fails


LockProvider lockProvider = new JdbcTemplateLockProvider(/* JdbcTemplate goes here */);

Map<String, StoredRecordConsumer> storedRecordConsumers = ConsumerUtils.toStoredRecordConsumerMap(
        consumers,
        new StringDeserializer(),
        new StringDeserializer()
);

KafkaConsumerRecordProcessor consumerRecordProcessor = new KafkaConsumerRecordProcessor(lockProvider, kafkaConsumerRepository, storedRecordConsumers);

consumerRecordProcessor.start(); // Will periodically consume stored messages
```

#### NB

Hvis en melding feiler, så vil andre meldinger med samme key på samme topic og partisjon ikke bli konsumert. Dette er for at meldinger ikke skal bli konsumert out-of-order.
Det vil derfor potensielt ligge 1 melding som blokker andre meldinger, f.eks. for en gitt bruker dersom man bruker ident som key.

### Metrikker

Det er lagt til støtte for et sett med default prometheus metrikker for consumer/producer.

Metrikker for consumer settes opp pr topic:
```java
MeterRegistry registry = /* ... */;

KafkaConsumerClient<String, String> consumerClient = KafkaConsumerClientBuilder.<String, String>builder()
        /* ... */
        .withMetrics(registry) // Will enable metrics for all topics configured on this client
        .withLogging() // (Optional) Will enable additional logging for all topics configured on this client
        /* ... */
        .build();
```

## Producer

### Basic

```java
Credentials credentials = new Credentials("username", "password");

KafkaProducerClient<String, String> producerClient = KafkaProducerClientBuilder.<String, String>builder()
        .withProps(KafkaProperties.onPremDefaultProducerProperties("producer_id", "broker_url", credentials))
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
KafkaProducerRepository producerRepository = new OracleProducerRepository(dataSource);

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
           .withProps(KafkaProperties.onPremByteProducerProperties("producer_id", "broker_url", credentials))
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
        .withProps(KafkaProperties.onPremDefaultProducerProperties("producer_id", "broker_url", credentials))
        .withMetrics(registry)
        .build();
```
