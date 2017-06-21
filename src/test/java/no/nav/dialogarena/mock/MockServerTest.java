package no.nav.dialogarena.mock;

import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;

import static no.nav.dialogarena.mock.MockServer.startMockServer;
import static org.eclipse.jetty.http.HttpMethod.POST;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;


public class MockServerTest {

    private static HttpClient httpClient;
    private static Jetty mockServer;
    private static int jettyPort;

    @BeforeClass
    public static void setup() throws Exception {
        httpClient = new HttpClient(new SslContextFactory());
        httpClient.start();

        jettyPort = freePort();
        mockServer = startMockServer("mockservertest", jettyPort);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        httpClient.stop();
        mockServer.stop.run();
    }

    @Test
    public void startMockServer_serverMockData() throws Exception {
        String getContent = httpClient.newRequest("http://localhost:" + jettyPort + "/mockservertest/sti/til/content").send().getContentAsString();
        assertThat(getContent, equalTo("GET-content"));

        String postContent = httpClient.newRequest("http://localhost:" + jettyPort + "/mockservertest/sti/til/content").method(POST).send().getContentAsString();
        assertThat(postContent, equalTo("POST-content"));
    }

    @Test
    public void startMockServer_finnerJsonFiler() throws Exception {
        String jsonContent = httpClient.newRequest("http://localhost:" + jettyPort + "/mockservertest/sti/til/jsoncontent").send().getContentAsString();
        assertThat(jsonContent, equalTo("{\"text\": \"json\"}"));
    }


    @Test
    public void startMockServer_taklerAtContextNameErContextPath() throws Exception {
        mockServer.stop.run();
        mockServer = startMockServer("/mockservertest", jettyPort);

        startMockServer_serverMockData();
        startMockServer_finnerJsonFiler();
    }

    @Test
    public void javascriptEngine_finnerParams() throws Exception {
        String jsonContent = httpClient.newRequest("http://localhost:" + jettyPort + "/mockservertest/sti/til/jscontent?fnr=123&param=noe").send().getContentAsString();
        assertThat(jsonContent, equalTo("{\"param\":\"noe\",\"fnr\":\"123\"}"));
    }

    @Test
    public void javascriptEngine_setterResponseStatus() throws Exception {
        int responseStatus = httpClient.newRequest("http://localhost:" + jettyPort + "/mockservertest/sti/til/jscontent?fnr=500").send().getStatus();
        assertThat(responseStatus, equalTo(500));
    }

    private static int freePort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

}