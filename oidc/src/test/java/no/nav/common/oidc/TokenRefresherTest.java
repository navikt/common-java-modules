package no.nav.common.oidc;

import no.nav.common.oidc.utils.TokenRefresher;
import no.nav.testconfig.security.JwtTestTokenIssuerConfig;
import no.nav.testconfig.security.OidcProviderTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.util.SocketUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenRefresherTest {

    private final static JwtTestTokenIssuerConfig issuerConfig = JwtTestTokenIssuerConfig.builder()
            .id("oidc-provider-test-rule-aadb2c")
            .issuer("oidc-provider-test-rule-aadb2c")
            .audience("oidc-provider-test-rule")
            .build();

    @Rule
    public OidcProviderTestRule oidcProviderRule = new OidcProviderTestRule(SocketUtils.findAvailableTcpPort(), issuerConfig);

    @Test
    public void shouldRefreshToken() {
        Optional<String> refreshedToken = TokenRefresher.refreshIdToken(oidcProviderRule.getRefreshUri(), "refresh-token");
        assertThat(refreshedToken.isPresent()).isTrue();
    }

    @Test
    public void shouldReturnEmptyIfWrongUrl() {
        Optional<String> refreshedToken = TokenRefresher.refreshIdToken("http://not-a-real-host.test/refresh", "refresh-token");
        assertThat(refreshedToken.isPresent()).isFalse();
    }

}
