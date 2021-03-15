# NAV common kafka

Formålet for denne modulen er å gjøre det lettere å integrere mot Kafka med sane defaults.
Det er også lagt til funksjonalitet for standard feilhåndtering samt forslag til konfigurering av kafka for konsumenter og produsenter.
Modulen er ikke avhengig av rammeverk som f.eks Spring og kan brukes uavhengig.


## Basic Producer

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

## Basic Consumer

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

## Producer med feilhåntering

Basic produceren er relativt enkel og vil i flere tilfeller være den foretrukkede måten og sende meldinger på.
F.eks hvis man ikke sender store mengder med meldinger og kan tåle at Kafka er nede, så kan man bruke `sendSync()`
som vil blocke eller kaste et exception hvis meldingen ikke ble sendt.

Hvis man sender asynkront så vil man ikke være garantert at meldignen er sendt med mindre man lagrer meldingen før man prøver å sende den ut.
Kafka-modulen inneholder en producer som implementerer store-and-forward patternet, som lagrer meldingen før den sender, og fjerner meldingen hvis sendingen gikk greit.

For å bruke store-and-forward så må det til en del mer konfigurasjon.



```java


```

## Consumer med feilhåndtering

## Metrikker

