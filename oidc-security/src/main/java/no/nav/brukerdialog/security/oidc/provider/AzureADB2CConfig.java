package no.nav.brukerdialog.security.oidc.provider;

import lombok.Builder;

import static no.nav.sbl.util.StringUtils.assertNotNullOrEmpty;

@Builder
public class AzureADB2CConfig {

    public final String discoveryUrl;
    public final String expectedAudience;

    private AzureADB2CConfig(String discoveryUrl, String expectedAudience) {
        this.discoveryUrl = assertNotNullOrEmpty(discoveryUrl);
        this.expectedAudience = expectedAudience;
    }

}
