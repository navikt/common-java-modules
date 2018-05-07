package no.nav.fo.feed.integration;

import no.nav.fo.feed.TestLockProvider;
import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.common.FeedWebhookRequest;
import no.nav.fo.feed.common.OutInterceptor;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.feed.producer.FeedProducer;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class IntegrationTest {

    private static final String PRODUCER_PORT = "31337";
    private static final String CONSUMER_PORT = "31338";

    private static List<OutInterceptor> outInterceptors = asList(
            (OutInterceptor) builder -> builder.header("interceptor1", "intercepted1"),
            (OutInterceptor) builder -> builder.header("interceptor2", "intercepted2")
    );
    private static List<FeedElement<DomainObject>> mockData = asList(
            new FeedElement<DomainObject>().setId("0").setElement(new DomainObject("0", "name 0")),
            new FeedElement<DomainObject>().setId("1").setElement(new DomainObject("1", "name 1")),
            new FeedElement<DomainObject>().setId("2").setElement(new DomainObject("2", "name 2")),
            new FeedElement<DomainObject>().setId("3").setElement(new DomainObject("3", "name 3")),
            new FeedElement<DomainObject>().setId("4").setElement(new DomainObject("4", "name 4")),
            new FeedElement<DomainObject>().setId("5").setElement(new DomainObject("5", "name 5")),
            new FeedElement<DomainObject>().setId("6").setElement(new DomainObject("6", "name 6")),
            new FeedElement<DomainObject>().setId("7").setElement(new DomainObject("7", "name 7"))
    );
    private Server producerServer;
    private Server consumerServer;

    @Before
    public void before() {
        producerServer = new Server(PRODUCER_PORT);
        consumerServer = new Server(CONSUMER_PORT);

        producerServer.controller.addFeed("testfeed", FeedProducer.<DomainObject>builder()
                .interceptors(outInterceptors)
                .provider((id, pageSize) -> mockData.stream())
                .build()
        );
    }

    @After
    public void after() {
        producerServer.server.shutdownNow();
        consumerServer.server.shutdownNow();
    }

    @Test
    public void rapportererFeedname() {
        producerServer.controller.addFeed("testfeed", FeedProducer.<DomainObject>builder().build());
        producerServer.controller.addFeed("anotherfeed", FeedProducer.<DomainObject>builder().build());

        Client client = ClientBuilder.newClient();
        Response response = client.target(basePath(PRODUCER_PORT)).path("feed/feedname").request().get();
        String body = response.readEntity(String.class);

        assertThat(body, Matchers.containsString("testfeed"));
        assertThat(body, Matchers.containsString("anotherfeed"));
    }

    @Test
    public void girRiktigStatusForWebhooks() {
        String callbackMedWebhook = basePath(CONSUMER_PORT) + "feed/medwebhook";
        String callbackUrlUtenWebhookKonfig = basePath(CONSUMER_PORT) + "feed/utenwebhook";
        String callbackSomIkkeFinnes = basePath(CONSUMER_PORT) + "feed/finnesikke";
        FeedConsumerConfig.BaseConfig<DomainObject> baseConfig = new FeedConsumerConfig.BaseConfig<>(
                DomainObject.class,
                () -> "0",
                basePath(PRODUCER_PORT).toString(),
                "webhook-producer"
        );
        FeedConsumerConfig.WebhookScheduleCreator webhookPollingConfig = new FeedConsumerConfig.CronWebhookPollingConfig("*/10 * * * * ?", "");
        FeedConsumerConfig<DomainObject> configMedConfig = new FeedConsumerConfig<>(baseConfig, null, webhookPollingConfig);
        FeedConsumerConfig<DomainObject> configUtenConfig = new FeedConsumerConfig<>(baseConfig, null, null);
        FeedConsumer<DomainObject> consumerUtenConfig = new FeedConsumer<>(configUtenConfig);
        FeedConsumer<DomainObject> consumerMedConfig = new FeedConsumer<>(configMedConfig);

        consumerServer.controller.addFeed("utenwebhook", consumerUtenConfig);
        consumerServer.controller.addFeed("medwebhook", consumerMedConfig);

        Client client = ClientBuilder.newClient();
        assertThat(client.target(callbackUrlUtenWebhookKonfig).request().head().getStatus(), is(404));
        assertThat(client.target(callbackSomIkkeFinnes).request().head().getStatus(), is(404));
        assertThat(client.target(callbackMedWebhook).request().head().getStatus(), is(200));

    }

    @Test
    public void girRiktigStatusForWebhooksGjennomProducer() throws InterruptedException, ExecutionException {
        String callbackMedWebhook = basePath(CONSUMER_PORT) + "feed/medwebhook";
        String callbackUrlUtenWebhookKonfig = basePath(CONSUMER_PORT) + "feed/utenwebhook";
        String callbackSomIkkeFinnes = basePath(CONSUMER_PORT) + "feed/finnesikke";
        FeedConsumerConfig.BaseConfig<DomainObject> baseConfig = new FeedConsumerConfig.BaseConfig<>(
                DomainObject.class,
                () -> "0",
                basePath(PRODUCER_PORT).toString(),
                "webhook-producer2"
        );
        FeedConsumerConfig.WebhookScheduleCreator webhookPollingConfig = new FeedConsumerConfig.CronWebhookPollingConfig("*/10 * * * * ?", "");
        FeedConsumerConfig<DomainObject> configMedConfig = new FeedConsumerConfig<>(baseConfig, null, webhookPollingConfig);
        FeedConsumerConfig<DomainObject> configUtenConfig = new FeedConsumerConfig<>(baseConfig, null, null);
        FeedConsumer<DomainObject> consumerUtenConfig = new FeedConsumer<>(configUtenConfig);
        FeedConsumer<DomainObject> consumerMedConfig = new FeedConsumer<>(configMedConfig);

        FeedProducer<DomainObject> producer = FeedProducer.<DomainObject>builder().build();
        producer.createWebhook(new FeedWebhookRequest().setCallbackUrl(callbackUrlUtenWebhookKonfig));
        producer.createWebhook(new FeedWebhookRequest().setCallbackUrl(callbackSomIkkeFinnes));
        producer.createWebhook(new FeedWebhookRequest().setCallbackUrl(callbackMedWebhook));
        consumerServer.controller.addFeed("utenwebhook", consumerUtenConfig);
        consumerServer.controller.addFeed("medwebhook", consumerMedConfig);

        Map<String, Future<Integer>> res = producer.activateWebhook();
        assertThat(res.get(callbackUrlUtenWebhookKonfig).get(), is(404));
        assertThat(res.get(callbackSomIkkeFinnes).get(), is(404));
        assertThat(res.get(callbackMedWebhook).get(), is(200));
    }

    @Test
    public void fullstendigOppsett() throws Exception {
        CountDownLatch lock = new CountDownLatch(1);
        FeedConsumerConfig.BaseConfig<DomainObject> baseConfig = new FeedConsumerConfig.BaseConfig<>(
                DomainObject.class,
                () -> "0",
                basePath(PRODUCER_PORT).toString(),
                "producer"
        );
        FeedConsumerConfig.ScheduleCreator pollingConfig = new FeedConsumerConfig.CronPollingConfig("*/10 * * * * ?");
        FeedConsumerConfig.WebhookScheduleCreator webhookPollingConfig = new FeedConsumerConfig.CronWebhookPollingConfig("*/10 * * * * ?", "/api");
        FeedConsumerConfig<DomainObject> consumerConfig = new FeedConsumerConfig<>(baseConfig, pollingConfig, webhookPollingConfig);

        final String[] respLastId = {null};
        final Integer[] respSize = {null};
        consumerConfig.callback((lastId, data) -> {
            respLastId[0] = lastId;
            respSize[0] = data.size();
            lock.countDown();
        });
        FeedConsumer<DomainObject> consumer = new FeedConsumer<>(consumerConfig);

        FeedProducer<DomainObject> producer = FeedProducer.<DomainObject>builder()
                .provider((id, pageSize) -> mockData.stream())
                .build();
        producer.createWebhook(new FeedWebhookRequest().setCallbackUrl(basePath(CONSUMER_PORT) + "feed/consumer"));
        producerServer.controller.addFeed("producer", producer);
        consumerServer.controller.addFeed("consumer", consumer);

        producer.activateWebhook();
        lock.await(10, TimeUnit.SECONDS);

        assertThat(respLastId[0], is("7"));
        assertThat(respSize[0], is(8));
    }

    @Test
    public void getSkalReturnereForbidden() {
        producerServer.controller.addFeed("testfeed", FeedProducer.<DomainObject>builder().authorizationModule((feedname) -> false).build());
        Client client = ClientBuilder.newClient();
        Response response = client.target(basePath(PRODUCER_PORT)).path("feed/testfeed").queryParam("id", "0").request().get();

        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void putWebhookSkalReturnereForbidden() {
        producerServer.controller.addFeed("testfeed", FeedProducer.<DomainObject>builder().authorizationModule((feedname) -> false).build());
        Client client = ClientBuilder.newClient();
        Response response = client.target(basePath(PRODUCER_PORT)).path("feed/testfeed/webhook").request().buildPut(Entity.json(new FeedWebhookRequest())).invoke();

        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void consumerSkalReturnerForbidden() {
        FeedConsumerConfig.BaseConfig<DomainObject> baseConfig = new FeedConsumerConfig.BaseConfig<>(
                DomainObject.class,
                null,
                basePath(CONSUMER_PORT).toString(),
                "webhook-producer"
        );

        FeedConsumerConfig<DomainObject> config = new FeedConsumerConfig<DomainObject>(baseConfig, null).authorizatioModule((feedname) -> false);

        consumerServer.controller.addFeed("testfeed", new FeedConsumer<DomainObject>(config));

        Client client = ClientBuilder.newClient();
        Response response = client.target(basePath(CONSUMER_PORT)).path("feed/testfeed").request().head();

        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void consumersShouldUseLocks() throws InterruptedException {
        FeedCallback<DomainObject> callback = mock(FeedCallback.class);
        TestLockProvider lockProvider = new TestLockProvider();

        FeedConsumerConfig.BaseConfig<DomainObject> baseConfig = new FeedConsumerConfig.BaseConfig<>(
                DomainObject.class,
                () -> "0",
                basePath(PRODUCER_PORT).toString(),
                "producerlocks"
        );

        FeedConsumerConfig.WebhookScheduleCreator webhookPollingConfig = new FeedConsumerConfig.SimpleWebhookPollingConfig(90, "/api");
        FeedConsumerConfig<DomainObject> consumerConfig = new FeedConsumerConfig<>(baseConfig, null, webhookPollingConfig);
        consumerConfig.lockProvider(lockProvider, 500);
        consumerConfig.callback(callback);

        FeedConsumer<DomainObject> consumer = new FeedConsumer<>(consumerConfig);
        FeedProducer<DomainObject> producer = FeedProducer.<DomainObject>builder()
                .provider((id, pageSize) -> {
                    delay(500);
                    return mockData.stream();
                })
                .build();
        producer.createWebhook(new FeedWebhookRequest().setCallbackUrl(basePath(CONSUMER_PORT) + "feed/consumerlocks"));
        producerServer.controller.addFeed("producerlocks", producer);
        consumerServer.controller.addFeed("consumerlocks", consumer);

        doAsync(producer::activateWebhook, 5, 5);
        Thread.sleep(1000);
        verify(callback, times(1)).call(anyString(), anyList());
        assertThat(TestLockProvider.locksGiven, is(1));

        doAsync(producer::activateWebhook, 5, 5);
        Thread.sleep(1000);
        assertThat(TestLockProvider.locksGiven, is(2));
        verify(callback, times(2)).call(anyString(), anyList());

        doAsync(producer::activateWebhook, 5, 5);
        producer.activateWebhook();
        Thread.sleep(1000);
        assertThat(TestLockProvider.locksGiven, is(3));
        verify(callback, times(3)).call(anyString(), anyList());
    }

    private void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void doAsync(Supplier<?> supplier, int count, int parallellism) {
        ForkJoinPool fjp = new ForkJoinPool(parallellism);
        List<Callable<Object>> tasks = IntStream.range(0, count)
                .mapToObj((i) -> ((Callable<Object>) () -> supplier.get()))
                .collect(toList());

        fjp.invokeAll(tasks);
    }

    static class Server {
        public final FeedController controller;
        public final HttpServer server;

        public Server(String port) {
            this.controller = new FeedController();
            ResourceConfig config = new ResourceConfig();
            config.property("contextConfig", new StaticApplicationContext());
            config.register(this.controller);
            this.server = GrizzlyHttpServerFactory.createHttpServer(basePath(port), config);
        }
    }

    static URI basePath(String port) {
        return URI.create(String.format("http://localhost:%s/api/", port));
    }
}