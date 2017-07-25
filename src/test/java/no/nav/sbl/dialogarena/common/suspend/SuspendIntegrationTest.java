package no.nav.sbl.dialogarena.common.suspend;


import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.DispatcherType;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.EnumSet;
import java.util.Properties;

import static javax.servlet.http.HttpServletResponse.*;
import static no.nav.sbl.dialogarena.common.suspend.BasicAuthenticationFilter.AUTH_PROPERTY_NAME;
import static no.nav.sbl.dialogarena.common.suspend.SuspendServlet.SHUTDOWN_TIME;
import static org.assertj.core.api.Assertions.assertThat;

public class SuspendIntegrationTest {


    public static final String LOCALHOST = "http://localhost:8080";
    public static final String SERVLET_IS_ALIVE = "/internal/isAlive";
    public static final String SERVLET_SUSPEND = "/management/suspend";

    public static final String IS_ALIVE_URL = LOCALHOST + SERVLET_IS_ALIVE;
    public static final String SUSPEND_URL = LOCALHOST + SERVLET_SUSPEND;
    public static final String AUTH_PROPERTY = "authyauthy";
    public static final String USERNAME = "suspender";
    public static final String PASSWORD = "pwd";
    public static final String AUTH = USERNAME+":"+PASSWORD;
    public static final String WRONG_AUTH = USERNAME+":feilpassord";

    private Server server;

    @Before
    public void setup() throws Exception {
        server = new Server(8080);
        final Properties properties = new Properties();
        properties.setProperty(AUTH_PROPERTY + ".username", USERNAME);
        properties.setProperty(AUTH_PROPERTY + ".password", PASSWORD);
        server.addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {
                                        @Override
                                        public void lifeCycleStarting(LifeCycle event) {
                                            System.getProperties().putAll(properties);
                                        }
                                    });
        ServletContextHandler context = new ServletContextHandler();

        FilterHolder authFilter = new FilterHolder(BasicAuthenticationFilter.class);
        authFilter.setInitParameter(AUTH_PROPERTY_NAME, AUTH_PROPERTY);
        context.addFilter(authFilter, SERVLET_SUSPEND, EnumSet.of(DispatcherType.REQUEST));

        context.addServlet(IsAliveServlet.class, SERVLET_IS_ALIVE);

        ServletHolder suspendServlet = new ServletHolder(SuspendServlet.class);
        suspendServlet.setInitParameter(SHUTDOWN_TIME, "500");
        context.addServlet(suspendServlet, SERVLET_SUSPEND);

        server.setHandler(context);
        server.start();
    }

    @Test
    public void signalisererIsAliveEtterOppstart() throws IOException {
        HttpURLConnection http = GETisAlive();
        assertResponseCode(http, SC_OK);
    }

    @Test
    public void signalisererIkkeISuspendModus() throws IOException {
        HttpURLConnection http = authenticatedGETSuspend();
        assertResponseCode(http, SC_SERVICE_UNAVAILABLE);
    }

    @Test
    public void skalFaFeilUtenPaloggingGET() throws IOException {
        HttpURLConnection http = GETSuspend();
        assertResponseCode(http, SC_UNAUTHORIZED);
    }

    @Test
    public void skalFaFeilUtenPaloggingPUT() throws IOException {
        HttpURLConnection http = PUTSuspend();
        assertResponseCode(http, SC_UNAUTHORIZED);
    }

    @Test
    public void skalFaFeilMedFeilPassord() throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(SUSPEND_URL).openConnection();
        httpURLConnection.setRequestMethod("GET");
        String basicAuth = "Basic " + new String(Base64.encodeBase64(WRONG_AUTH.getBytes()));
        httpURLConnection.setRequestProperty("Authorization", basicAuth);
        httpURLConnection.connect();
        HttpURLConnection http = httpURLConnection;

        assertResponseCode(http, SC_UNAUTHORIZED);
    }

    @Test(timeout = 8000L)
    public void signaliserTilIsAliveVedAktivSuspendModus() throws IOException, InterruptedException {
        assertResponseCode(authenticatedPUTSuspend(), SC_OK);

        assertResponseCode(authenticatedGETSuspend(), SC_SERVICE_UNAVAILABLE);
        assertResponseCode(GETisAlive(), SC_SERVICE_UNAVAILABLE);

        while (authenticatedGETSuspend().getResponseCode() != SC_OK) {

        }
        assertResponseCode(authenticatedGETSuspend(), SC_OK);
        assertResponseCode(authenticatedPUTSuspend(), SC_OK);
        assertResponseCode(authenticatedGETSuspend(), SC_OK);
        assertResponseCode(GETisAlive(), SC_SERVICE_UNAVAILABLE);
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    private void assertResponseCode(HttpURLConnection httpURLConnection, int responseCode) throws IOException {
        assertThat(httpURLConnection.getResponseCode()).isEqualTo(responseCode);
    }

    private HttpURLConnection GETisAlive() throws IOException {
        return getHttpURLConnection(IS_ALIVE_URL, "GET");
    }

    private HttpURLConnection authenticatedGETSuspend() throws IOException {
        return getAuthenticatedHttpURLConnection(SUSPEND_URL, "GET");
    }

    private HttpURLConnection GETSuspend() throws IOException {
        return getHttpURLConnection(SUSPEND_URL, "GET");
    }

    private HttpURLConnection PUTSuspend() throws IOException {
        return getHttpURLConnection(SUSPEND_URL, "PUT");
    }

    private HttpURLConnection authenticatedPUTSuspend() throws IOException {
        return getAuthenticatedHttpURLConnection(SUSPEND_URL, "PUT");
    }

    private static HttpURLConnection getHttpURLConnection(String url, String requestMethod) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
        httpURLConnection.setRequestMethod(requestMethod);
        httpURLConnection.connect();
        return httpURLConnection;
    }

    private static HttpURLConnection getAuthenticatedHttpURLConnection(String url, String requestMethod) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
        httpURLConnection.setRequestMethod(requestMethod);
        String basicAuth = "Basic " + new String(Base64.encodeBase64(AUTH.getBytes()));
        httpURLConnection.setRequestProperty("Authorization", basicAuth);
        httpURLConnection.connect();
        return httpURLConnection;
    }


}
