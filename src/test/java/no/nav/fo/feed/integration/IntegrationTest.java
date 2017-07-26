package no.nav.fo.feed.integration;

import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.common.OutInterceptor;
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
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class IntegrationTest {
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
        producerServer = new Server("31337");
        consumerServer = new Server("31338");

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
        Response response = client.target(basePath("31337")).path("feed/feedname").request().get();
        String body = response.readEntity(String.class);

        assertThat(body, Matchers.containsString("testfeed"));
        assertThat(body, Matchers.containsString("anotherfeed"));
    }

    @Test
    public void girRiktigStatusForWebhooks() throws Exception {
        String callbackMedWebhook = basePath("31338") + "feed/medwebhook";
        String callbackUrlUtenWebhookKonfig = basePath("31338") + "feed/utenwebhook";
        String callbackSomIkkeFinnes = basePath("31338") + "feed/finnesikke";
        FeedConsumerConfig.BaseConfig<DomainObject> baseConfig = new FeedConsumerConfig.BaseConfig<>(
                DomainObject.class,
                () -> "0",
                basePath("31337").toString(),
                "webhook-producer"
        );
        FeedConsumerConfig.WebhookPollingConfig webhookPollingConfig = new FeedConsumerConfig.WebhookPollingConfig("*/10 * * * * ?", "");
        FeedConsumerConfig<DomainObject> configMedConfig = new FeedConsumerConfig<>(baseConfig, webhookPollingConfig);
        FeedConsumerConfig<DomainObject> configUtenConfig = new FeedConsumerConfig<>(baseConfig, (FeedConsumerConfig.PollingConfig) null);
        FeedConsumer<DomainObject> consumerUtenConfig = new FeedConsumer<>(configUtenConfig);
        FeedConsumer<DomainObject> consumerMedConfig = new FeedConsumer<>(configMedConfig);
        FeedProducer<DomainObject> producer = FeedProducer.<DomainObject>builder()
                .callbackUrls(asList(callbackUrlUtenWebhookKonfig, callbackSomIkkeFinnes, callbackMedWebhook))
                .build();
        consumerServer.controller.addFeed("utenwebhook", consumerUtenConfig);
        consumerServer.controller.addFeed("medwebhook", consumerMedConfig);

        Map<String, Integer> res = producer.activateWebhook();
        assertThat(res.get(callbackUrlUtenWebhookKonfig), is(404));
        assertThat(res.get(callbackSomIkkeFinnes), is(404));
        assertThat(res.get(callbackMedWebhook), is(200));
    }

    @Test
    public void fullstendigOppsett() throws Exception {
        CountDownLatch lock = new CountDownLatch(1);
        FeedConsumerConfig.BaseConfig<DomainObject> baseConfig = new FeedConsumerConfig.BaseConfig<>(
                DomainObject.class,
                () -> "0",
                basePath("31337").toString(),
                "producer"
        );
        FeedConsumerConfig.PollingConfig pollingConfig = new FeedConsumerConfig.PollingConfig("*/10 * * * * ?");
        FeedConsumerConfig.WebhookPollingConfig webhookPollingConfig = new FeedConsumerConfig.WebhookPollingConfig("*/10 * * * * ?", "/api");
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
                .callbackUrls(asList(basePath("31338") + "feed/consumer"))
                .provider((id, pageSize) -> mockData.stream())
                .build();

        producerServer.controller.addFeed("producer", producer);
        consumerServer.controller.addFeed("consumer", consumer);

        producer.activateWebhook();
        lock.await(10, TimeUnit.SECONDS);

        assertThat(respLastId[0], is("7"));
        assertThat(respSize[0], is(8));
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