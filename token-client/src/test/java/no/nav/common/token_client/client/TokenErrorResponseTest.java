package no.nav.common.token_client.client;

import static no.nav.common.token_client.test_utils.Constants.TEST_JWK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.util.function.Function;
import java.util.stream.Stream;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import no.nav.common.token_client.test_utils.TokenCreator;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.LoggerFactory;

class TokenErrorResponseTest {

    @TestFactory
    Stream<DynamicTest> skal_kun_eksponere_tillatte_feilkoder() {
        return Stream.of("AzureAD M2M", "AzureAD OBO", "TokenX OBO")
                .flatMap(client -> Stream.of(
                                new Feilrespons(
                                        "beskrivelse_og_ekstrafelt",
                                        "{\"error\":\"invalid_grant\",\"error_description\":\"sensitiv-testverdi\","
                                                + "\"access_token\":\"sensitiv-testverdi\","
                                                + "\"error_uri\":\"https://example.invalid/sensitiv-testverdi\"}",
                                        "invalid_grant"),
                                new Feilrespons(
                                        "ukjent_feilkode",
                                        "{\"error\":\"sensitiv-testverdi\",\"error_description\":\"sensitiv-testverdi\"}",
                                        "unrecognized_error"),
                                new Feilrespons(
                                        "ugyldig_json", "{\"error\":\"sensitiv-testverdi\"", "invalid_response"),
                                new Feilrespons(
                                        "manglende_feilkode",
                                        "{\"error_description\":\"sensitiv-testverdi\"}",
                                        "invalid_response"),
                                new Feilrespons(
                                        "ugyldig_suksessrespons",
                                        "{\"token_type\":\"Bearer\",\"access_token\":{"
                                                + "\"sensitiv-testverdi\":\"sensitiv-testverdi\"}}",
                                        "invalid_response",
                                        200),
                                new Feilrespons(
                                        "respons_uten_json",
                                        "<html>sensitiv-testverdi</html>",
                                        "invalid_response",
                                        500))
                        .map(response -> DynamicTest.dynamicTest(
                                client + "_" + response.navn(), () -> sjekkFeilrespons(client, response))));
    }

    private void sjekkFeilrespons(String client, Feilrespons response) throws IOException {
        Logger logger = (Logger) LoggerFactory.getLogger("no.nav.common.token_client.client");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse.Builder()
                    .code(response.status())
                    .addHeader("Content-Type", "application/json")
                    .body(response.body())
                    .build());

            String endpoint = server.url("/token").toString();
            String token = TokenCreator.instance().createToken("syntetisk-testbruker");
            Function<String, String> request = switch (client) {
                case "AzureAD M2M" ->
                    new AzureAdMachineToMachineTokenClient("test-id", endpoint, TEST_JWK, null)
                            ::createMachineToMachineToken;
                case "AzureAD OBO" ->
                    scope -> new AzureAdOnBehalfOfTokenClient("test-id", endpoint, TEST_JWK, null)
                            .exchangeOnBehalfOfToken(scope, token);
                case "TokenX OBO" ->
                    scope -> new TokenXOnBehalfOfTokenClient("test-id", endpoint, TEST_JWK, null)
                            .exchangeOnBehalfOfToken(scope, token);
                default -> throw new IllegalArgumentException(client);
            };

            RuntimeException exception =
                    assertThrows(RuntimeException.class, () -> request.apply("sensitiv-testverdi"));
            String expected =
                    client + " token request failed: HTTP " + response.status() + ", error=" + response.feilkode();
            assertEquals(expected, exception.getMessage());
            assertNull(exception.getCause());
            assertEquals(1, appender.list.size());
            assertEquals(expected, appender.list.getFirst().getFormattedMessage());
            assertNull(appender.list.getFirst().getThrowableProxy());
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private record Feilrespons(String navn, String body, String feilkode, int status) {
        private Feilrespons(String navn, String body, String feilkode) {
            this(navn, body, feilkode, 400);
        }
    }
}
