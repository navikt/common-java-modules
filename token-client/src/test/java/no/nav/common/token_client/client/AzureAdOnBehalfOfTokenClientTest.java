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

public class AzureAdOnBehalfOfTokenClientTest {

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
        String accessToken = TokenCreator.instance().createToken();

        server.enqueue(tokenMockResponse(accessToken));

        AzureAdOnBehalfOfTokenClient tokenClient = new AzureAdOnBehalfOfTokenClient(
                "test-id",
                server.url("/token").toString(),
                TEST_JWK,
                new CaffeineTokenCache()
        );

        String token = tokenClient.exchangeOnBehalfOfToken("test-scope", TokenCreator.instance().createToken());

        RecordedRequest recordedRequest = server.takeRequest();

        Map<String, String> data = parseFormData(recordedRequest.getBody().readUtf8());

        assertEquals(accessToken, token);
        assertEquals("/token", recordedRequest.getPath());
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("urn:ietf:params:oauth:client-assertion-type:jwt-bearer", data.get("client_assertion_type"));
        assertEquals("test-scope", data.get("audience"));
        assertEquals("urn:ietf:params:oauth:grant-type:jwt-bearer", data.get("grant_type"));
        assertEquals("test-scope", data.get("scope"));
        assertNotNull(data.get("client_assertion"));
    }

    @Test
    public void should_cache_request() {
        server.enqueue(tokenMockResponse(TokenCreator.instance().createToken()));
        server.enqueue(tokenMockResponse(TokenCreator.instance().createToken()));
        server.enqueue(tokenMockResponse(TokenCreator.instance().createToken()));

        AzureAdOnBehalfOfTokenClient tokenClient = new AzureAdOnBehalfOfTokenClient(
                "test-id",
                server.url("/token").toString(),
                TEST_JWK,
                new CaffeineTokenCache()
        );

        String token1 = TokenCreator.instance().createToken();
        String token2 = TokenCreator.instance().createToken();

        tokenClient.exchangeOnBehalfOfToken("test-scope", token1);
        tokenClient.exchangeOnBehalfOfToken("test-scope", token1);

        assertEquals(1, server.getRequestCount());

        tokenClient.exchangeOnBehalfOfToken("test-scope", token2);
        tokenClient.exchangeOnBehalfOfToken("test-scope-2", token2);

        assertEquals(3, server.getRequestCount());
    }

}
