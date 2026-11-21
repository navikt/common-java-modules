package no.nav.common.token_client.client;

import static java.util.Objects.requireNonNull;
import static no.nav.common.token_client.test_utils.Constants.TEST_JWK;
import static no.nav.common.token_client.test_utils.RequestUtils.parseFormData;
import static no.nav.common.token_client.test_utils.RequestUtils.tokenMockResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

public class TokenXOnBehalfOfTokenClientTest {
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
        String subjectToken = TokenCreator.instance().createToken("innkommende-bruker");

        server.enqueue(tokenMockResponse(accessToken));

        var tokenClient = new TokenXOnBehalfOfTokenClient(
                "test-id", server.url("/token").toString(), TEST_JWK, new CaffeineTokenCache());

        // Act
        String token = tokenClient.exchangeOnBehalfOfToken("test-scope", subjectToken);

        RecordedRequest recordedRequest = server.takeRequest();

        Map<String, List<String>> data =
                parseFormData(requireNonNull(recordedRequest.getBody(), "Token-requesten mangler body")
                        .utf8());

        // Assert
        assertEquals(accessToken, token);
        assertEquals("/token", recordedRequest.getTarget());
        assertEquals("POST", recordedRequest.getMethod());
        assertNotNull(data.get("client_assertion"));
        assertEquals(
                Map.of(
                        "client_assertion_type", List.of("urn:ietf:params:oauth:client-assertion-type:jwt-bearer"),
                        "client_assertion", List.of(data.get("client_assertion").getFirst()),
                        "audience", List.of("test-scope"),
                        "grant_type", List.of("urn:ietf:params:oauth:grant-type:token-exchange"),
                        "scope", List.of("test-scope"),
                        "subject_token", List.of(subjectToken),
                        "subject_token_type", List.of("urn:ietf:params:oauth:token-type:jwt")),
                data);
    }

    @Test
    public void should_cache_request() {
        // Arrange
        server.enqueue(tokenMockResponse(TokenCreator.instance().createToken("user-1")));
        server.enqueue(tokenMockResponse(TokenCreator.instance().createToken("user-2")));
        server.enqueue(tokenMockResponse(TokenCreator.instance().createToken("user-2")));

        var tokenClient = new TokenXOnBehalfOfTokenClient(
                "test-id", server.url("/token").toString(), TEST_JWK, new CaffeineTokenCache());

        String token1 = TokenCreator.instance().createToken("user-1");
        String token2 = TokenCreator.instance().createToken("user-2");
        String token3 = TokenCreator.instance().createToken("user-1");

        // Act
        tokenClient.exchangeOnBehalfOfToken("test-scope", token1);
        tokenClient.exchangeOnBehalfOfToken("test-scope", token1);
        tokenClient.exchangeOnBehalfOfToken("test-scope", token3);

        // Assert
        assertEquals(1, server.getRequestCount());

        // Act
        tokenClient.exchangeOnBehalfOfToken("test-scope", token2);
        tokenClient.exchangeOnBehalfOfToken("test-scope-2", token2);

        // Assert
        assertEquals(3, server.getRequestCount());
    }

    @Test
    public void should_throw_exception_if_subject_missing() {
        // Arrange
        var tokenClient = new TokenXOnBehalfOfTokenClient(
                "test-id", server.url("/token").toString(), TEST_JWK, new CaffeineTokenCache());

        String token = TokenCreator.instance().createToken(null);

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> tokenClient.exchangeOnBehalfOfToken("test-scope", token),
                "Unable to get subject, access token is missing subject");
    }
}
