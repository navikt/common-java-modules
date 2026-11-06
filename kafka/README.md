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

### Spring lifecycle (opt-in)

`no.nav.common.kafka.spring.KafkaConsumerClientLifecycle` implements `SmartLifecycle`.
It follows this module's existing optional Spring integration convention: `spring-context`
is optional, and neither the core API nor the builder depends on Spring or Spring Boot.
Spring applications must provide `spring-context` themselves (normally through their
existing Spring dependencies). There is no component scanning registration or auto-configuration.
Upgrading the library alone does not start existing clients.

Register one adapter for the clients it exclusively owns. A coordinated group starts
in list order and stops in reverse order. If a start fails, it stops the failing client
(which may be partially initialized) and previously started clients. The original
exception is rethrown, with cleanup errors suppressed. This provides explicit rollback
within the group rather than relying on independent adapters being stopped after a failed refresh.
Duplicate client instances in a group are rejected. Do not register the same client
in multiple groups or mix manual and lifecycle ownership.

#### Construction and startup contract

Investigation of `KafkaConsumerClientBuilder`, `KafkaConsumerClientImpl`, and
`FeatureToggledKafkaConsumerClient` established:

- `build()` validates configuration, constructs handler wrappers, and tags supplied metrics
  listeners. The core constructor registers a JVM shutdown hook. Neither constructs a
  Kafka consumer, starts polling, or invokes a message handler.
- `start()` synchronously constructs a `KafkaConsumer`, subscribes, and submits its polling
  worker. Kafka client construction can initialize serializers, interceptors, security,
  DNS, and client-internal resources, and can fail. The common client does not wait for
  broker connectivity, metadata, or partition assignment. Those happen through polling.
  Custom Kafka initialization code can still block; this is not a hard latency guarantee.
- Handlers can execute as soon as the worker polls records, potentially before `start()`
  returns. Consequently, invoking `start()` from an ordinary `@Bean` factory method
  can process messages before context initialization is complete.
- A feature toggle is sampled immediately on start and subsequently every minute by
  default. When disabled, polling is stopped but the toggle scheduler remains active
  so consumption can resume. Its existing `isRunning()` reports underlying polling state.

The adapter starts synchronously in Spring's lifecycle stage of **context refresh**,
after regular non-lazy singleton initialization, not after `ApplicationReadyEvent`.
It does not move startup onto another executor or eagerly retrieve all beans.
Its `isRunning()` means all owned `start()` calls returned successfully and lifecycle
ownership is active, even if a toggle currently disables every consumer. It does not
mean Kafka is reachable, assignments exist, or processing is healthy.

The default phase is `SmartLifecycle.DEFAULT_PHASE` (`Integer.MAX_VALUE`).
Lower-phase infrastructure starts first and stops last; the constructor also accepts
an explicit phase. Existing `JdbcTemplate` repositories are ordinary initialized beans,
not independent lifecycle services: their data sources and handler dependencies must
be injected before construction completes. Context close normally stops lifecycle
beans before destroying singletons. For infrastructure that itself implements
`Lifecycle`/`SmartLifecycle`, choose a lower phase or declare explicit bean dependencies
(for example `@DependsOn`) if it uses the same phase. There is no universal phase that
orders unrelated infrastructure with equal or higher phases.
Declare required migration/initialization beans as explicit adapter dependencies too;
a phase alone cannot wait for an unrelated asynchronous service to become ready.
Lazy handler dependencies must be explicitly injected or declared as dependencies;
the adapter cannot discover dependencies hidden inside a callback or service locator.

No readiness or liveness indicator is added. Applications decide whether Kafka
availability is required for their own readiness. No startup-performance improvement
has been measured or is claimed.

#### Shutdown and compatibility

Repeated lifecycle notifications are serialized and do not duplicate workers.
`stop(Runnable)` invokes its callback once per successful call, after all owned clients
have stopped. It propagates failures rather than acknowledging unfinished shutdown.
Other clients are still stopped if one fails; errors are aggregated and failed clients
remain tracked for a subsequent stop attempt. The adapter also implements `DisposableBean`
to clean up when refresh is cancelled without normal lifecycle shutdown.

Core stop requests stop intake, skips queued unstarted records, drains in-flight
handlers, commits successful offsets, and closes the Kafka consumer. Partition ordering,
failed-record retry, and successful-offset calculation are unchanged. A successful stop
also terminates partition/poll workers; restarting creates a fresh poll executor.
The toggle wrapper first drains its scheduler, then stops the underlying client, so a
toggle task cannot restart it after completed shutdown.
Unexpected worker exits still trigger automatic recovery, but now close the old
session and create a fresh Kafka consumer on the tracked worker instead of resubmitting
polling against an already closed consumer. Failed startup/close resources remain
owned until cleanup succeeds. Runtime toggle synchronization failures are logged and
retried on the next scheduled check instead of silently cancelling all future checks.

The core stop waits up to 10 seconds for its worker. A toggle wrapper additionally waits
up to 10 seconds for its scheduler, so allow approximately 20 seconds per toggled client
and sum those budgets for a group stopped sequentially. Final Kafka commit and close
each use a 3-second timeout within worker cleanup. Configure Spring's phase timeout
and the process termination grace period accordingly; for example, three toggled
clients need more than 60 seconds, not the usual short shutdown budget:

```yaml
spring:
  lifecycle:
    timeout-per-shutdown-phase: 75s
```

These are bounded **waits**, not a guarantee that arbitrary handler, toggle supplier,
or custom Kafka initialization code terminates. A timeout or interruption now throws
instead of silently reporting success; interruption status is restored.
Lifecycle calls are serialized, so a stop concurrent with startup first waits for that
startup call; lock contention is not included in the stop wait budgets.
Handlers are not forcibly interrupted or discarded. After a timeout, work may continue,
the callback is not acknowledged, and starting a replacement run is prohibited until
the old run has terminated. A later stop can retry cleanup. Spring may nevertheless
continue destroying dependencies after a shutdown failure. Applications must enforce
finite timeouts in their handlers/suppliers to guarantee complete shutdown before those
dependencies disappear. Spring's phase timeout cannot preempt a synchronous `stop()`.
Custom `KafkaConsumerClient` implementations supplied to the adapter must likewise
provide bounded, synchronous stop behavior.

#### Migrating an application such as veilarbaktivitet

A library upgrade does **not** remove the application's starting factory method.
Remove this bean entirely:

```kotlin
@Bean
open fun consumerClients(): List<KafkaConsumerClient> =
    kafkaConsumerClients.onEach { it.start() }
```

Keep the three existing construction beans, including their existing builder properties,
topic configs, handlers, and toggles. Add `destroyMethod = ""` to those bean declarations
so Spring does not infer a second `close`/`shutdown` owner. The following replacement
uses illustrative bean names; retain the application's real names and builder bodies:

```kotlin
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.spring.KafkaConsumerClientLifecycle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean

// Existing construction beans: retain each original builder body, without start().
// @Bean(destroyMethod = "")
// open fun firstConsumer(): KafkaConsumerClient = ...existingBuilder.build()
// @Bean(destroyMethod = "")
// open fun secondConsumer(): KafkaConsumerClient = ...existingBuilder.build()
// @Bean(destroyMethod = "")
// open fun thirdConsumer(): KafkaConsumerClient = ...existingBuilder.build()

@Bean(destroyMethod = "")
open fun navKafkaConsumerLifecycle(
    @Qualifier("firstConsumer") first: KafkaConsumerClient,
    @Qualifier("secondConsumer") second: KafkaConsumerClient,
    @Qualifier("thirdConsumer") third: KafkaConsumerClient,
): KafkaConsumerClientLifecycle =
    KafkaConsumerClientLifecycle(listOf(first, second, third))
```

The adapter is now the sole application-level startup/shutdown owner. `DisposableBean`
still runs even with destroy-method inference disabled. The core's existing JVM stop
hook is idempotent and remains a fallback, not an ordered Spring shutdown mechanism.
Remove any other explicit consumer start/stop hooks. Do not change Spring Kafka
`@KafkaListener` consumers; Spring Kafka already owns their lifecycle.
This repository change does not migrate or claim to fix veilarbaktivitet itself.

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
