package no.nav.dialogarena.mock;

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

    @BeforeClass
    public static void setup() throws Exception {
        httpClient = new HttpClient(new SslContextFactory());
        httpClient.start();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        httpClient.stop();
    }

    @Test
    public void startMockServer_serverMockData() throws Exception {
        int jettyPort = freePort();
        startMockServer("mockservertest", jettyPort);

        String getContent = httpClient.newRequest("http://localhost:" + jettyPort + "/mockservertest/sti/til/content").send().getContentAsString();
        assertThat(getContent, equalTo("GET-content"));

        String postContent = httpClient.newRequest("http://localhost:" + jettyPort + "/mockservertest/sti/til/content").method(POST).send().getContentAsString();
        assertThat(postContent, equalTo("POST-content"));
    }

    private int freePort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

}