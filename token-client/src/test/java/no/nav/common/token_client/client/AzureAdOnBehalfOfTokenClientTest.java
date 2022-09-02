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
import static org.junit.jupiter.api.Assertions.*;

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
        String accessToken = TokenCreator.instance().createToken("test");

        server.enqueue(tokenMockResponse(accessToken));

        AzureAdOnBehalfOfTokenClient tokenClient = new AzureAdOnBehalfOfTokenClient(
                "test-id",
                server.url("/token").toString(),
                TEST_JWK,
                new CaffeineTokenCache()
        );

        String token = tokenClient.exchangeOnBehalfOfToken("test-scope", TokenCreator.instance().createToken("test"));

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
        server.enqueue(tokenMockResponse(TokenCreator.instance().createToken("user-1")));
        server.enqueue(tokenMockResponse(TokenCreator.instance().createToken("user-2")));
        server.enqueue(tokenMockResponse(TokenCreator.instance().createToken("user-2")));

        AzureAdOnBehalfOfTokenClient tokenClient = new AzureAdOnBehalfOfTokenClient(
                "test-id",
                server.url("/token").toString(),
                TEST_JWK,
                new CaffeineTokenCache()
        );

        String token1 = TokenCreator.instance().createToken("user-1");
        String token2 = TokenCreator.instance().createToken("user-2");
        String token3 = TokenCreator.instance().createToken("user-1");

        tokenClient.exchangeOnBehalfOfToken("test-scope", token1);
        tokenClient.exchangeOnBehalfOfToken("test-scope", token1);
        tokenClient.exchangeOnBehalfOfToken("test-scope", token3);

        assertEquals(1, server.getRequestCount());

        tokenClient.exchangeOnBehalfOfToken("test-scope", token2);
        tokenClient.exchangeOnBehalfOfToken("test-scope-2", token2);

        assertEquals(3, server.getRequestCount());
    }

    @Test
    public void should_throw_exception_if_subject_missing() {
        AzureAdOnBehalfOfTokenClient tokenClient = new AzureAdOnBehalfOfTokenClient(
                "test-id",
                server.url("/token").toString(),
                TEST_JWK,
                new CaffeineTokenCache()
        );

        String token = TokenCreator.instance().createToken(null);

        assertThrows(IllegalArgumentException.class, () -> {
            tokenClient.exchangeOnBehalfOfToken("test-scope", token);
        }, "Unable to get subject, access token is missing subject");
    }

}
