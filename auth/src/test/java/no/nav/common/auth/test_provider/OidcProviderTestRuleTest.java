package no.nav.common.auth.test_provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import no.nav.common.json.JsonUtils;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class OidcProviderTestRuleTest {
    private static final JwtTestTokenIssuerConfig CONFIG = JwtTestTokenIssuerConfig.builder()
            .id("oidc-rule-test")
            .issuer("oidc-rule-test")
            .audience("test-audience")
            .build();

    @Test
    public void krever_serverstart_foer_url_er_tilgjengelig() {
        // Arrange
        OidcProviderTestRule rule = new OidcProviderTestRule(CONFIG);

        // Act
        Throwable discoveryFailure = catchThrowable(rule::getDiscoveryUri);
        Throwable jwksFailure = catchThrowable(rule::getJwksUri);
        Throwable refreshFailure = catchThrowable(rule::getRefreshUri);

        // Assert
        assertThat(discoveryFailure).isInstanceOf(IllegalStateException.class);
        assertThat(jwksFailure).isInstanceOf(IllegalStateException.class);
        assertThat(refreshFailure).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void starter_flere_servere_med_egne_porter_og_riktige_endepunkter() throws Exception {
        // Arrange
        OidcProviderTestRule first = new OidcProviderTestRule(CONFIG);
        OidcProviderTestRule second = new OidcProviderTestRule(CONFIG);
        try (HttpClient client = HttpClient.newHttpClient()) {
            try {
                // Act
                first.before();
                second.before();
                HttpResponse<String> discovery = get(client, first.getDiscoveryUri());
                HttpResponse<String> jwks = get(client, first.getJwksUri());
                HttpResponse<String> secondDiscovery = get(client, second.getDiscoveryUri());
                HttpResponse<String> refresh = client.send(
                        HttpRequest.newBuilder(URI.create(first.getRefreshUri()))
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                // Assert
                assertThat(first.getDiscoveryUri()).isNotEqualTo(second.getDiscoveryUri());
                assertThat(discovery.statusCode()).isEqualTo(200);
                assertThat(JsonUtils.getMapper()
                                .readTree(discovery.body())
                                .get("issuer")
                                .asString())
                        .isEqualTo(CONFIG.issuer);
                assertThat(JsonUtils.getMapper()
                                .readTree(discovery.body())
                                .get("jwks_uri")
                                .asString())
                        .isEqualTo(first.getJwksUri());
                assertThat(jwks.statusCode()).isEqualTo(200);
                assertThat(JsonUtils.getMapper()
                                .readTree(jwks.body())
                                .get("keys")
                                .size())
                        .isPositive();
                assertThat(secondDiscovery.statusCode()).isEqualTo(200);
                assertThat(JsonUtils.getMapper()
                                .readTree(secondDiscovery.body())
                                .get("jwks_uri")
                                .asString())
                        .isEqualTo(second.getJwksUri());
                assertThat(refresh.statusCode()).isEqualTo(200);
                assertThat(JsonUtils.getMapper()
                                .readTree(refresh.body())
                                .get("idToken")
                                .asString())
                        .isNotBlank();
            } finally {
                second.after();
                first.after();
            }
        }
    }

    @Test
    public void stopper_serveren_selv_naar_testen_feiler() {
        // Arrange
        OidcProviderTestRule rule = new OidcProviderTestRule(CONFIG);
        AssertionError failure = new AssertionError("expected failure");
        String[] runningUri = new String[1];
        Statement evaluation = rule.apply(
                new Statement() {
                    @Override
                    public void evaluate() {
                        runningUri[0] = rule.getDiscoveryUri();
                        throw failure;
                    }
                },
                Description.EMPTY);

        // Act
        Throwable result = catchThrowable(evaluation::evaluate);
        Throwable stoppedFailure = catchThrowable(rule::getDiscoveryUri);

        // Assert
        assertThat(result).isSameAs(failure);
        assertThat(runningUri[0]).startsWith("http://localhost:");
        assertThat(stoppedFailure).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void kan_starte_paa_nytt_med_oppdatert_discovery() throws Exception {
        // Arrange
        OidcProviderTestRule rule = new OidcProviderTestRule(CONFIG);
        try (HttpClient client = HttpClient.newHttpClient()) {
            try {
                rule.before();
                rule.after();

                // Act
                rule.before();
                HttpResponse<String> discovery = get(client, rule.getDiscoveryUri());

                // Assert
                assertThat(discovery.statusCode()).isEqualTo(200);
                assertThat(JsonUtils.getMapper()
                                .readTree(discovery.body())
                                .get("jwks_uri")
                                .asString())
                        .isEqualTo(rule.getJwksUri());
            } finally {
                rule.after();
            }
        }
    }

    private static HttpResponse<String> get(HttpClient client, String uri) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(uri)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
