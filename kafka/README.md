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
            return ConsumeStatus.OK;
        })
        .build();

consumerClient.start();

// Records will be consumed from topic1 and topic2
```

### Feilhåndtering

Feilhåndtering for consumer settes opp pr topic ved bruk av `StoreOnFailureTopicConsumer`. 
Feilhåndteringen vil sørge for at meldinger blir lagret hvis det feiler under konsumeringen, f.eks hvis en ekstern tjeneste feiler for en bestemt bruker.
Hvis feilhåndtering ikke brukes så vil konsumeringen stå stille helt til konsumeringen går igjennom.

Hvordan sette opp topic med feilhåndtering:
```java
KafkaConsumerRepository<String, String> consumerRepository = new OracleConsumerRepository<>(
        dataSource,
        new StringSerializer(),
        new StringDeserializer(),
        new StringSerializer(),
        new StringDeserializer()
);

TopicConsumer<String, String> consumer = TopicConsumerBuilder.<String, String>builder()
        .withConsumer((record -> ConsumeStatus.OK))
        .withStoreOnFailure(consumerRepository)
        .build();

KafkaConsumerClient<String, String> consumerClient = KafkaConsumerClientBuilder.<String, String>builder()
        .withProps(KafkaProperties.defaultConsumerProperties("group_id", "broker_url", credentials))
        .withTopic("topic1", consumer)
        .build();
```

Hvordan sette opp retry for feilede konsumerte meldinger:
```java
Map<String, TopicConsumer<String, String>> topics = new HashMap<>();
// Add topics to the map

KafkaRetryConsumerRecordHandler<String, String> retryConsumerHandler = new KafkaRetryConsumerRecordHandler<>(topics, consumerRepository);

// This should be used periodically in a schedule
retryConsumerHandler.consumeFailedMessages();
```

#### NB

Siden meldingene lagres til databasen og vi går videre til å konsumere neste melding så må det også lages en periodisk jobb for å rekonsumere feilede meldinger.

En ting som er viktig å være obs på er at når man lagrer unna feilede meldinger, så mister man garantien for at meldinger blir lest inn i riktig rekkefølge.

F.eks 
```
    Melding 1 for bruker med fnr: 123 -> Konsumering feiler, lagrer i databasen
    Melding 2 for bruker med fnr: 456 -> Konsumering fullført, trenger ikke å lagre
    Periodisk jobb -> Prøver å konsumere melding 1 på nytt, feiler fortsatt
    Melding 3 for bruker med fnr: 123 -> Konsumering fullført, trenger ikke å lagre
    Periodisk jobb -> Prøver å konsumere melding 1 på nytt, melding blir sendt
```

I dette tilfellet så blir melding 1 publisert etter melding 3, som kan føre til problemer hvis konsumenten ikke håndterer dette riktig.

### Metrikker

Det er lagt til støtte for et sett med default prometheus metrikker for consumer/producer.

Metrikker for consumer settes opp pr topic:
```java
MeterRegistry registry = /* ... */;

TopicConsumer<String, String> consumer1 = TopicConsumerBuilder.<String, String>builder()
        .withConsumer((record -> ConsumeStatus.OK))
        .withMetrics(registry)
        .withLogging() // Kan også legge på optional logging til stdout
        .build();
```


## ===============================


## Producer

### Basic

```java
Credentials credentials = new Credentials("username", "password");

KafkaProducerClient<String, String> producerClient = KafkaProducerClientBuilder.<String, String>builder()
        .withProps(KafkaProperties.defaultProducerProperties("group_id", "broker_url", credentials))
        .build();

// Send synchronously. Will block until sent or throw an exception
producerClient.sendSync(new ProducerRecord<>("topic", "key", "value"));

// Send asynchronously. Will batch up records and send after a short time has passed. Callback is triggered for both failure and success
producerClient.send(new ProducerRecord<>("topic", "key", "value"), ((metadata, exception) -> { /* ... */ }));
```

### Feilhåntering

Basic produceren er relativt enkel og vil i flere tilfeller være den foretrukkede måten og sende meldinger på.
F.eks hvis man ikke sender store mengder med meldinger og kan tåle at Kafka er nede, så kan man bruke `sendSync()`
som vil blocke eller kaste et exception hvis meldingen ikke ble sendt.

Hvis man sender asynkront så vil man ikke være garantert at meldignen er sendt med mindre man lagrer meldingen før man prøver å sende den ut.
Kafka-modulen inneholder en producer som implementerer store-and-forward patternet, som lagrer meldingen før den sender, og fjerner meldingen hvis sendingen gikk greit.

For å bruke store-and-forward så må det til en del mer konfigurasjon. Først så må tabellene settes opp slik at vi kan lagre meldingene når ting feiler.
Nedenfor så ligger det lenker til ferdig SQL som kan brukes for Oracle og PostgreSQL.

[Oracle](src/test/resources/kafka-producer-record-oracle.sql)

[Postgres](src/test/resources/kafka-producer-record-postgres.sql)

Deretter så må det settes opp med builderen når man lager produceren at man ønsker å bruke store-and-forward.

```java
DataSource dataSource = null; // Must be retrieved from somewhere
        
KafkaProducerRepository<String, String> producerRepository = new OracleProducerRepository<>(
        dataSource,
        new StringSerializer(),
        new StringDeserializer(),
        new StringSerializer(),
        new StringDeserializer()
);

KafkaProducerClient<String, String> producerClient = KafkaProducerClientBuilder.<String, String>builder()
        .withProps(KafkaProperties.defaultProducerProperties("group_id", "broker_url", credentials))
        .withStoreAndForward(producerRepository)
        .build();
```

Dette vil sørge for at meldingene blir lagret i databasen først før man sender og at de blir fjernet hvis meldingen ble sendt.
Men hvis kafka er nede så trengs det også logikk for å periodisk sjekke om det ligger meldinger i databasen som ikke har blitt lagt ut på kafka.

```java
// It is important to not use the store-and-forward producer with KafkaRetryProducerRecordHandler, 
//  or else a new record will be stored in the database if the retry handler fails to send a record
KafkaProducerClient<String, String> producerClient = KafkaProducerClientBuilder.<String, String>builder()
        .withProps(KafkaProperties.defaultProducerProperties("group_id", "broker_url", credentials))
        .build();

KafkaRetryProducerRecordHandler<String, String> retryProducerRecordHandler = new KafkaRetryProducerRecordHandler<>(List.of("topic1"), producerRepository, producerClient);

// This should be used periodically in a schedule
retryProducerRecordHandler.sendFailedMessages();
```
### Metrikker

Metrikker for producer kan settes opp gjennom builder:
```java
MeterRegistry registry = /* ... */;

KafkaProducerClient<String, String> producerClient = KafkaProducerClientBuilder.<String, String>builder()
        .withProps(KafkaProperties.defaultProducerProperties("group_id", "broker_url", credentials))
        .withMetrics(registry)
        .build();
```
