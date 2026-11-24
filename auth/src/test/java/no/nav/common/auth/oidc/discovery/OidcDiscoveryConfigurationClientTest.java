package no.nav.common.auth.oidc.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import no.nav.common.auth.test_provider.JwtTestTokenIssuerConfig;
import no.nav.common.auth.test_provider.OidcProviderTestRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class OidcDiscoveryConfigurationClientTest {

    private static final JwtTestTokenIssuerConfig issuerConfig = JwtTestTokenIssuerConfig.builder()
            .id("oidc-provider-test-rule-aadb2c")
            .issuer("oidc-provider-test-rule-aadb2c")
            .audience("oidc-provider-test-rule")
            .build();

    @RegisterExtension
    public OidcProviderTestRule oidcProviderRule = new OidcProviderTestRule(issuerConfig);

    @Test
    public void shouldRetrieveDiscoveryConfig() {
        OidcDiscoveryConfigurationClient client = new OidcDiscoveryConfigurationClient();
        OidcDiscoveryConfiguration config = client.fetchDiscoveryConfiguration(oidcProviderRule.getDiscoveryUri());

        assertThat(config.issuer).matches("oidc-provider-test-rule-aadb2c");
        assertThat(config.jwksUri).matches(oidcProviderRule.getJwksUri());
    }

    @Test
    public void shouldThrowIfWrongUrl() {
        OidcDiscoveryConfigurationClient client = new OidcDiscoveryConfigurationClient();
        assertThrows(
                Exception.class, () -> client.fetchDiscoveryConfiguration("http://not-a-real-host.test/discovery"));
    }
}
