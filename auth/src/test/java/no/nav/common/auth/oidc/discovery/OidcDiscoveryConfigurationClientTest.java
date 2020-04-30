package no.nav.common.auth.oidc.discovery;

import no.nav.common.auth.test_provider.JwtTestTokenIssuerConfig;
import no.nav.common.auth.test_provider.OidcProviderTestRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OidcDiscoveryConfigurationClientTest {

    private final static JwtTestTokenIssuerConfig issuerConfig = JwtTestTokenIssuerConfig.builder()
            .id("oidc-provider-test-rule-aadb2c")
            .issuer("oidc-provider-test-rule-aadb2c")
            .audience("oidc-provider-test-rule")
            .build();

    @Rule
    public OidcProviderTestRule oidcProviderRule = new OidcProviderTestRule(issuerConfig);

    @Test
    public void shouldRetrieveDiscoveryConfig() {
        OidcDiscoveryConfigurationClient client = new OidcDiscoveryConfigurationClient();
        OidcDiscoveryConfiguration config = client.fetchDiscoveryConfiguration(oidcProviderRule.getDiscoveryUri());

        assertThat(config.issuer).matches("oidc-provider-test-rule-aadb2c");
        assertThat(config.jwksUri).matches(oidcProviderRule.getJwksUri());
    }

    @Test(expected = Exception.class)
    public void shouldThrowIfWrongUrl() {
        OidcDiscoveryConfigurationClient client = new OidcDiscoveryConfigurationClient();
        client.fetchDiscoveryConfiguration("http://not-a-real-host.test/discovery");
    }

}
