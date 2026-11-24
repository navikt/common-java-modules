package no.nav.common.auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nimbusds.jwt.JWTParser;
import java.text.ParseException;
import no.nav.common.auth.test_provider.JwtTestTokenIssuerConfig;
import no.nav.common.auth.test_provider.OidcProviderTestRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TokenRefreshClientTest {

    private static final JwtTestTokenIssuerConfig issuerConfig = JwtTestTokenIssuerConfig.builder()
            .id("oidc-provider-test-rule-aadb2c")
            .issuer("oidc-provider-test-rule-aadb2c")
            .audience("oidc-provider-test-rule")
            .build();

    @RegisterExtension
    public OidcProviderTestRule oidcProviderRule = new OidcProviderTestRule(issuerConfig);

    @Test
    public void shouldRefreshToken() {
        // Arrange
        var refreshClient = new TokenRefreshClient();

        // Act & Assert
        assertThatCode(() -> refreshClient.refreshIdToken(oidcProviderRule.getRefreshUri(), "refresh-token"))
                .doesNotThrowAnyException();
    }

    @Test
    public void skal_utstede_et_nytt_token_for_hver_refresh() throws ParseException {
        // Arrange
        var refreshClient = new TokenRefreshClient();

        // Act
        String firstToken = refreshClient.refreshIdToken(oidcProviderRule.getRefreshUri(), "refresh-token");
        String secondToken = refreshClient.refreshIdToken(oidcProviderRule.getRefreshUri(), "refresh-token");

        // Assert
        assertThat(secondToken).isNotEqualTo(firstToken);
        var claims = JWTParser.parse(secondToken).getJWTClaimsSet();
        assertThat(claims.getSubject()).isEqualTo("subject");
        assertThat(claims.getIssuer()).isEqualTo(issuerConfig.issuer);
        assertThat(claims.getAudience()).containsExactly(issuerConfig.audience);
    }

    @Test
    public void shouldThrowIfWrongUrl() {
        // Arrange
        var refreshClient = new TokenRefreshClient();

        // Act & Assert
        assertThrows(
                Exception.class,
                () -> refreshClient.refreshIdToken("http://not-a-real-host.test/refresh", "refresh-token"));
    }
}
