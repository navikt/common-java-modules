package no.nav.fo.apiapp;

import no.nav.apiapp.rest.JsonProvider;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static no.nav.apiapp.ServletUtil.getContext;


public abstract class JettyTest {

    static {
        StartJetty.setupLogging();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(JettyTest.class);

    protected static final String CONTEXT_NAME = JettyTest.class.getSimpleName();
    protected static final Jetty JETTY = StartJetty.nyJetty(CONTEXT_NAME, tilfeldigPort());

    private Client client = ClientBuilder.newBuilder().register(new JsonProvider()).build();
    private Map<String, NewCookie> cookies = new HashMap<>();

    @BeforeClass
    public static void startJetty() {
        JETTY.start();
    }

    @AfterClass
    public static void stopJetty() {
        JETTY.stop.run();
    }

    @BeforeClass
    public static void systemUser() {
        System.setProperty("no.nav.modig.security.systemuser.username","username");
        System.setProperty("no.nav.modig.security.systemuser.password","password");
    }

    private static int tilfeldigPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Response get(String path) {
        URI uri = uri(path);
        LOGGER.info("get: {}", uri);
        Invocation.Builder request = client.target(uri).request();
        cookies.forEach((k, v) -> request.cookie(k, v.getValue()));
        Response response = request.get();
        response.getCookies().forEach((k, v) -> cookies.put(k, v));
        LOGGER.info("[response] status={} cookies={}", response.getStatus(), cookies);
        return response;
    }

    protected WebTarget target(String path) {
        URI uri = uri(path);
        return client.target(uri);
    }

    protected static URI uri(String path) {
        return UriBuilder.fromPath(CONTEXT_NAME + path).host(getHostName()).scheme("http").port(getPort()).build();
    }

    protected String getString(String path) {
        return get(path).readEntity(String.class);
    }

    protected <T> T getBean(Class<T> aClass) {
        WebApplicationContext webApplicationContext = getContext(JETTY.context.getServletContext());
        return webApplicationContext.getBean(aClass);
    }

    protected Map<String, NewCookie> getCookies() {
        return cookies;
    }

    private static int getPort() {
        return ((ServerConnector) JETTY.server.getConnectors()[0]).getPort();
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

}
