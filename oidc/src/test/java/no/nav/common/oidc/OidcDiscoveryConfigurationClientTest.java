package no.nav.common.oidc;

import no.nav.common.oidc.discovery.OidcDiscoveryConfiguration;
import no.nav.common.oidc.discovery.OidcDiscoveryConfigurationClient;
import no.nav.testconfig.security.JwtTestTokenIssuerConfig;
import no.nav.testconfig.security.OidcProviderTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.util.SocketUtils;

import java.util.Optional;

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
        Optional<OidcDiscoveryConfiguration> config = client.fetchDiscoveryConfiguration(oidcProviderRule.getDiscoveryUri());

        assertThat(config.isPresent()).isTrue();
        assertThat(config.get().issuer).matches("oidc-provider-test-rule-aadb2c");
        assertThat(config.get().jwksUri).matches(oidcProviderRule.getJwksUri());
    }

}
