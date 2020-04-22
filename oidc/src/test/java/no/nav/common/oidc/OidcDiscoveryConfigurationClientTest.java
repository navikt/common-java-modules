package no.nav.common.oidc;

import no.nav.common.oidc.discovery.OidcDiscoveryConfiguration;
import no.nav.common.oidc.discovery.OidcDiscoveryConfigurationClient;
import no.nav.common.oidc.test_provider.JwtTestTokenIssuerConfig;
import no.nav.common.oidc.test_provider.OidcProviderTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class OidcDiscoveryConfigurationClientTest {

    private final static JwtTestTokenIssuerConfig issuerConfig = JwtTestTokenIssuerConfig.builder()
            .id("oidc-provider-test-rule-aadb2c")
            .issuer("oidc-provider-test-rule-aadb2c")
            .audience("oidc-provider-test-rule")
            .build();

    @Rule
    public OidcProviderTestRule oidcProviderRule = new OidcProviderTestRule(SocketUtils.findAvailableTcpPort(), issuerConfig);

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
