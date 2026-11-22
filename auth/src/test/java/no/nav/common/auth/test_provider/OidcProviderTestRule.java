package no.nav.common.auth.test_provider;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import java.util.Map;
import no.nav.common.json.JsonUtils;
import org.junit.rules.ExternalResource;

public class OidcProviderTestRule extends ExternalResource {
    private final int port;
    private final WireMockServer httpServer;
    private final JwtTestTokenIssuerConfig issuerConfig;
    private final JwtTestTokenIssuer issuer;

    public OidcProviderTestRule(JwtTestTokenIssuerConfig issuerConfig) {
        this.port = PortUtils.findAvailablePort();
        this.issuerConfig = issuerConfig;
        this.issuer = JwtTestTokenUtil.testTokenIssuer(issuerConfig);
        this.httpServer = new WireMockServer(wireMockConfig().port(port).extensions(new RefreshResponseTransformer()));
    }

    @Override
    protected void before() {
        httpServer.stubFor(get(urlPathEqualTo("/discovery"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(JsonUtils.toJson(Map.of("issuer", issuerConfig.issuer, "jwks_uri", getJwksUri())))));

        httpServer.stubFor(get(urlPathEqualTo("/jwks"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(issuer.getKeySetJson())));

        httpServer.stubFor(
                post(urlPathEqualTo("/refresh")).willReturn(aResponse().withTransformers("oidc-refresh")));

        httpServer.start();
    }

    @Override
    protected void after() {
        httpServer.stop();
    }

    public String getToken(JwtTestTokenIssuer.Claims claims) {
        return issuer.issueTestToken(claims);
    }

    public String getAudience() {
        return issuerConfig.audience;
    }

    public String getDiscoveryUri() {
        return basePath() + "/discovery";
    }

    public String getJwksUri() {
        return basePath() + "/jwks";
    }

    public String getRefreshUri() {
        return basePath() + "/refresh";
    }

    private String basePath() {
        return "http://localhost:" + port;
    }

    private class RefreshResponseTransformer implements ResponseDefinitionTransformerV2 {

        @Override
        public ResponseDefinition transform(ServeEvent serveEvent) {
            String idToken = issuer.issueTestToken(new JwtTestTokenIssuer.Claims("subject"));
            return aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(JsonUtils.toJson(Map.of("idToken", idToken)))
                    .build();
        }

        @Override
        public String getName() {
            return "oidc-refresh";
        }

        @Override
        public boolean applyGlobally() {
            return false;
        }
    }
}
