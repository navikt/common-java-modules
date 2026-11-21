package no.nav.common.token_client.client;

import static java.util.Objects.requireNonNull;
import static no.nav.common.token_client.test_utils.Constants.TEST_JWK;
import static no.nav.common.token_client.test_utils.RequestUtils.parseFormData;
import static no.nav.common.token_client.test_utils.RequestUtils.tokenMockResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import no.nav.common.token_client.cache.CaffeineTokenCache;
import no.nav.common.token_client.test_utils.TokenCreator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AzureAdMachineToMachineTokenClientTest {

    private MockWebServer server;

    @BeforeEach
    public void start() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    public void shutdown() {
        server.close();
    }

    @Test
    public void skal_lage_riktig_request_og_parse_response() throws InterruptedException {
        // Arrange
        String accessToken = TokenCreator.instance().createToken("test");

        server.enqueue(tokenMockResponse(accessToken));

        var tokenClient = new AzureAdMachineToMachineTokenClient(
                "test-id", server.url("/token").toString(), TEST_JWK, new CaffeineTokenCache());

        // Act
        String token = tokenClient.createMachineToMachineToken("test-scope");

        RecordedRequest recordedRequest = server.takeRequest();

        Map<String, List<String>> data =
                parseFormData(requireNonNull(recordedRequest.getBody(), "Token-requesten mangler body")
                        .utf8());

        // Assert
        assertEquals(accessToken, token);
        assertEquals("/token", recordedRequest.getTarget());
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals(
                List.of("urn:ietf:params:oauth:client-assertion-type:jwt-bearer"), data.get("client_assertion_type"));
        assertEquals(List.of("test-scope"), data.get("audience"));
        assertEquals(List.of("client_credentials"), data.get("grant_type"));
        assertEquals(List.of("test-scope"), data.get("scope"));
        assertNotNull(data.get("client_assertion"));
        assertEquals(1, data.get("client_assertion").size());
    }

    @Test
    public void should_cache_request() {
        // Arrange
        server.enqueue(tokenMockResponse(TokenCreator.instance().createToken("test-1")));
        server.enqueue(tokenMockResponse(TokenCreator.instance().createToken("test-2")));

        var tokenClient = new AzureAdMachineToMachineTokenClient(
                "test-id", server.url("/token").toString(), TEST_JWK, new CaffeineTokenCache());

        // Act
        tokenClient.createMachineToMachineToken("test-scope");
        tokenClient.createMachineToMachineToken("test-scope");

        // Assert
        assertEquals(1, server.getRequestCount());

        // Act
        tokenClient.createMachineToMachineToken("test-scope-2");

        // Assert
        assertEquals(2, server.getRequestCount());
    }
}
