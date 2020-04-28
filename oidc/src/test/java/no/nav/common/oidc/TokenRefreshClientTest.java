package no.nav.common.oidc;

import no.nav.common.oidc.test_provider.JwtTestTokenIssuerConfig;
import no.nav.common.oidc.test_provider.OidcProviderTestRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

public class TokenRefreshClientTest {

    private final static JwtTestTokenIssuerConfig issuerConfig = JwtTestTokenIssuerConfig.builder()
            .id("oidc-provider-test-rule-aadb2c")
            .issuer("oidc-provider-test-rule-aadb2c")
            .audience("oidc-provider-test-rule")
            .build();

    @Rule
    public OidcProviderTestRule oidcProviderRule = new OidcProviderTestRule(issuerConfig);

    @Test
    public void shouldRefreshToken() {
        TokenRefreshClient refreshClient = new TokenRefreshClient();
        assertThatCode(() -> refreshClient.refreshIdToken(oidcProviderRule.getRefreshUri(), "refresh-token"))
                .doesNotThrowAnyException();
    }

    @Test(expected = Exception.class)
    public void shouldThrowIfWrongUrl() {
        TokenRefreshClient refreshClient = new TokenRefreshClient();
        refreshClient.refreshIdToken("http://not-a-real-host.test/refresh", "refresh-token");
    }

}
