package no.nav.common.token_client.client;

import no.nav.common.token_client.cache.CaffeineTokenCache;
import no.nav.common.token_client.test_utils.TokenCreator;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static no.nav.common.token_client.test_utils.Constants.TEST_JWK;
import static no.nav.common.token_client.test_utils.RequestUtils.parseFormData;
import static no.nav.common.token_client.test_utils.RequestUtils.tokenMockResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AzureAdMachineToMachineTokenClientTest {

    private MockWebServer server;

    @BeforeEach
    public void start() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    public void shutdown() throws IOException {
        server.shutdown();
    }

    @Test
    public void skal_lage_riktig_request_og_parse_response() throws InterruptedException {
        String accessToken = TokenCreator.instance().createToken("test");

        server.enqueue(tokenMockResponse(accessToken));

        AzureAdMachineToMachineTokenClient tokenClient = new AzureAdMachineToMachineTokenClient(
                "test-id",
                server.url("/token").toString(),
                TEST_JWK,
                new CaffeineTokenCache()
        );

        String token = tokenClient.createMachineToMachineToken("test-scope");

        RecordedRequest recordedRequest = server.takeRequest();

        Map<String, String> data = parseFormData(recordedRequest.getBody().readUtf8());

        assertEquals(accessToken, token);
        assertEquals("/token", recordedRequest.getPath());
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("urn:ietf:params:oauth:client-assertion-type:jwt-bearer", data.get("client_assertion_type"));
        assertEquals("test-scope", data.get("audience"));
        assertEquals("client_credentials", data.get("grant_type"));
        assertEquals("test-scope", data.get("scope"));
        assertNotNull(data.get("client_assertion"));
    }

    @Test
    public void should_cache_request() {
        server.enqueue(tokenMockResponse(TokenCreator.instance().createToken("test-1")));
        server.enqueue(tokenMockResponse(TokenCreator.instance().createToken("test-2")));

        AzureAdMachineToMachineTokenClient tokenClient = new AzureAdMachineToMachineTokenClient(
                "test-id",
                server.url("/token").toString(),
                TEST_JWK,
                new CaffeineTokenCache()
        );

        tokenClient.createMachineToMachineToken("test-scope");
        tokenClient.createMachineToMachineToken("test-scope");

        assertEquals(1, server.getRequestCount());

        tokenClient.createMachineToMachineToken("test-scope-2");

        assertEquals(2, server.getRequestCount());
    }


}
