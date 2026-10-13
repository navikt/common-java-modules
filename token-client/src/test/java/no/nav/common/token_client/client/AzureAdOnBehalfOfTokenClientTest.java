package no.nav.common.token_client.client;

import no.nav.common.token_client.builder.AzureAdTokenClientBuilder;
import no.nav.common.token_client.cache.AzureAdOnBehalfOfCacheKeyStrategy;
import no.nav.common.token_client.cache.CaffeineTokenCache;
import no.nav.common.token_client.cache.TokenCache;
import no.nav.common.token_client.test_utils.TokenCreator;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
        server.close();
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

        Map<String, String> data = parseFormData(recordedRequest.getBody().utf8());

        assertEquals(accessToken, token);
        assertEquals("/token", recordedRequest.getTarget());
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

    @Test
    public void should_use_subject_keys_by_default_with_injected_cache() {
        AtomicReference<String> capturedKey = new AtomicReference<>();
        TokenCache cache = (key, provider) -> {
            capturedKey.set(key);
            return "cached-token";
        };
        AzureAdOnBehalfOfTokenClient tokenClient = configuredBuilder().withCache(cache).buildOnBehalfOfTokenClient();

        assertEquals("cached-token",
                tokenClient.exchangeOnBehalfOfToken("test-scope", TokenCreator.instance().createToken("user-1")));
        assertEquals("test-scope-user-1", capturedKey.get());
        assertEquals(0, server.getRequestCount());
    }

    @Test
    public void should_use_assertion_keys_when_selected_with_injected_cache() {
        AtomicReference<String> capturedKey = new AtomicReference<>();
        TokenCache cache = (key, provider) -> {
            capturedKey.set(key);
            return "cached-token";
        };
        AzureAdOnBehalfOfTokenClient tokenClient = configuredBuilder()
                .withCache(cache)
                .withOnBehalfOfCacheKeyStrategy(AzureAdOnBehalfOfCacheKeyStrategy.ASSERTION_HASH)
                .buildOnBehalfOfTokenClient();
        String incomingToken = TokenCreator.instance().createToken("user-1");

        assertEquals("cached-token", tokenClient.exchangeOnBehalfOfToken("test-scope", incomingToken));
        assertTrue(capturedKey.get().matches("azure-obo:v1:[0-9a-f]{64}"));
        assertFalse(capturedKey.get().contains("user-1"));
        assertFalse(capturedKey.get().contains("test-scope"));
        assertEquals(0, server.getRequestCount());
    }

    @Test
    public void should_cache_same_assertion_but_separate_assertions_for_same_subject() {
        String firstToken = TokenCreator.instance().createToken("user-1");
        String secondToken = TokenCreator.instance().createToken("user-1");
        String firstOutgoingToken = TokenCreator.instance().createToken("user-1");
        String secondOutgoingToken = TokenCreator.instance().createToken("user-1");
        server.enqueue(tokenMockResponse(firstOutgoingToken));
        server.enqueue(tokenMockResponse(secondOutgoingToken));
        AzureAdOnBehalfOfTokenClient tokenClient = configuredBuilder()
                .withOnBehalfOfCacheKeyStrategy(AzureAdOnBehalfOfCacheKeyStrategy.ASSERTION_HASH)
                .buildOnBehalfOfTokenClient();

        assertEquals(firstOutgoingToken, tokenClient.exchangeOnBehalfOfToken("test-scope", firstToken));
        assertEquals(firstOutgoingToken, tokenClient.exchangeOnBehalfOfToken("test-scope", firstToken));
        assertEquals(secondOutgoingToken, tokenClient.exchangeOnBehalfOfToken("test-scope", secondToken));
        assertEquals(2, server.getRequestCount());
    }

    @Test
    public void should_reject_missing_subject_with_assertion_keys() {
        AzureAdOnBehalfOfTokenClient tokenClient = configuredBuilder()
                .withOnBehalfOfCacheKeyStrategy(AzureAdOnBehalfOfCacheKeyStrategy.ASSERTION_HASH)
                .buildOnBehalfOfTokenClient();

        assertThrows(IllegalArgumentException.class, () -> tokenClient.exchangeOnBehalfOfToken(
                "test-scope", TokenCreator.instance().createToken(null)));
        assertEquals(0, server.getRequestCount());
    }

    @Test
    public void should_allow_disabling_cache_with_assertion_keys() {
        String outgoingToken = TokenCreator.instance().createToken("user-1");
        server.enqueue(tokenMockResponse(outgoingToken));
        server.enqueue(tokenMockResponse(outgoingToken));
        AzureAdOnBehalfOfTokenClient tokenClient = configuredBuilder()
                .withCache(null)
                .withOnBehalfOfCacheKeyStrategy(AzureAdOnBehalfOfCacheKeyStrategy.ASSERTION_HASH)
                .buildOnBehalfOfTokenClient();
        String incomingToken = TokenCreator.instance().createToken("user-1");

        assertEquals(outgoingToken, tokenClient.exchangeOnBehalfOfToken("test-scope", incomingToken));
        assertEquals(outgoingToken, tokenClient.exchangeOnBehalfOfToken("test-scope", incomingToken));
        assertEquals(2, server.getRequestCount());
    }

    @Test
    public void should_reject_null_cache_key_strategy() {
        assertThrows(NullPointerException.class, () -> configuredBuilder().withOnBehalfOfCacheKeyStrategy(null));
    }

    private AzureAdTokenClientBuilder configuredBuilder() {
        return AzureAdTokenClientBuilder.builder()
                .withClientId("test-id")
                .withTokenEndpointUrl(server.url("/token").toString())
                .withPrivateJwk(TEST_JWK);
    }

}
