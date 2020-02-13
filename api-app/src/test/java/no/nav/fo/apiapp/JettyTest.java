package no.nav.fo.apiapp;

import lombok.SneakyThrows;
import no.nav.fo.apiapp.rest.JettyTestUtils;
import no.nav.json.JsonProvider;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.util.EnvironmentUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.client.*;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static no.nav.apiapp.ServletUtil.getContext;
import static no.nav.fo.apiapp.rest.JettyTestUtils.*;
import static no.nav.log.LogFilter.CONSUMER_ID_HEADER_NAME;
import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;


public class JettyTest {

    private static JettyTest jettyTest;

    @BeforeClass
    public static void setUp() {
        jettyTest = new JettyTest().startJetty();
    }

    @AfterClass
    public static void tearDown() {
        jettyTest.stopJetty();
    }

    public static boolean DISABLE_AUTH = EnvironmentUtils.getOptionalProperty("DISABLE_AUTH").map(Boolean::parseBoolean).orElse(false);
    public static final String APPLICATION_NAME = "api-app";

    private Client client = ClientBuilder.newBuilder()
            .register(new JsonProvider())
            .property(ClientProperties.FOLLOW_REDIRECTS, "false")
            .build();

    private Map<String, NewCookie> cookies = new HashMap<>();


    public JettyTest() {
        setupContext();
    }

    public JettyTest(JettyTestConfig testConfig) {
        setupContext(testConfig);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(JettyTest.class);

    private static Jetty JETTY;

    @SneakyThrows
    public void stopJetty() {
        if (jettyTest == null || JETTY == null) {
            return;
        }
        JETTY.server.stop();
        JETTY = null;
    }

    public JettyTest startJetty() {
        if (JETTY == null) {
            DISABLE_AUTH = true;
            JETTY = nyJettyForTest(ApplicationConfig.class);
            Runtime.getRuntime().addShutdownHook(new Thread(JETTY.stop::run));
        }
        return this;
    }

    protected Response get(String path) {
        return request(path, SyncInvoker::get);
    }

    protected Response get(URI uri) {
        return request(uri, SyncInvoker::get);
    }

    protected Response request(String path, Function<Invocation.Builder, Response> ex) {
        return request(uri(path), ex);
    }

    protected Response request(URI uri, Function<Invocation.Builder, Response> ex) {
        LOGGER.info("request: {}", uri);
        Invocation.Builder request = client.target(uri)
                .request()
                .header(PREFERRED_NAV_CALL_ID_HEADER_NAME, "callId")
                .header(CONSUMER_ID_HEADER_NAME, "consumerId");
        cookies.forEach((k, v) -> request.cookie(k, v.getValue()));
        Response response = ex.apply(request);
        response.getCookies().forEach((k, v) -> cookies.put(k, v));
        LOGGER.info("[response] status={} cookies={}", response.getStatus(), cookies);
        return response;
    }

    protected WebTarget target(String path) {
        URI uri = uri(path);
        return client.target(uri);
    }

    protected static URI uri(String path) {
        return buildUri(path).build();
    }

    protected static UriBuilder buildUri(String path) {
        return UriBuilder.fromPath(APPLICATION_NAME + path).host(getHostName()).scheme("https").port(getSslPort());
    }

    protected String getString(String path) {
        return get(path).readEntity(String.class);
    }

    protected String putJson(String path, String jsonPayload) {
        return put(path, Entity.entity(jsonPayload, APPLICATION_JSON_TYPE)).readEntity(String.class);
    }

    protected Response put(String path, Entity<String> entity) {
        return request(path, r -> r.put(entity));
    }

    protected String postJson(String path, String jsonPayload) {
        return request(path, r -> r.post(Entity.entity(jsonPayload, APPLICATION_JSON_TYPE))).readEntity(String.class);
    }

    protected <T> T getBean(Class<T> aClass) {
        WebApplicationContext webApplicationContext = getContext(JETTY.context.getServletContext());
        return webApplicationContext.getBean(aClass);
    }

    protected Map<String, NewCookie> getCookies() {
        return cookies;
    }

    protected static int getSslPort() {
        return JettyTestUtils.getSslPort(JETTY);
    }
}
