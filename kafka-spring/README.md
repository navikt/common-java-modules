# NAV common kafka spring

Inneholder hjelpeklasser for bruk av [kafka-modulen](../kafka/README.md) med Spring


## Installering

```xml
<dependency>
    <groupId>no.nav.common</groupId>
    <artifactId>kafka-spring</artifactId>
</dependency>
```

## Feilhåndtering

Repositoryklasser, for Postgres og Oracle databaser, for lagring av feilede meldinger.

### Consumer
Feilhåndtering for konsumere er anbefalt i de tilfellene hvor det kan oppstå feil i koden som konsumerer meldinger 
og man ønsker å lagre den feilende meldingen og fortsette å konsumere slik at ting ikke stopper opp.
Feilhåndtering for consumer settes opp pr topic ved bruk av `StoreOnFailureTopicConsumer`. Dette kan enten gjøres manuelt, eller ved bruk av `KafkaConsumerClientBuilder.TopicConfig`.

For å lagre feilede meldinger så trengs det å legge til nye tabeller ved bruk av migreringsscript.

[Oracle](src/test/resources/kafka-consumer-record-oracle.sql)

[Postgres](src/test/resources/kafka-consumer-record-postgres.sql)

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


### Producer
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