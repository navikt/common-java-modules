package no.nav.common.sts.utils;

import no.nav.common.sts.AzureAdScopedTokenProvider;
import no.nav.common.sts.AzureAdServiceTokenProvider;
import no.nav.common.sts.CachedScopedTokenProvider;
import no.nav.common.sts.ScopedTokenProvider;
import no.nav.common.utils.EnvironmentUtils;

import static no.nav.common.sts.utils.AzureAdEnvironmentVariables.*;

public class AzureAdServiceTokenProviderBuilder {

    private String clientId;

    private String clientSecret;

    private String tokenEndpointUrl;

    private AzureAdServiceTokenProviderBuilder() {}

    public static AzureAdServiceTokenProviderBuilder builder() {
        return new AzureAdServiceTokenProviderBuilder();
    }

    public AzureAdServiceTokenProviderBuilder withEnvironmentDefaults() {
        clientId = EnvironmentUtils.getOptionalProperty(AZURE_APP_CLIENT_ID).orElse(null);
        clientSecret = EnvironmentUtils.getOptionalProperty(AZURE_APP_CLIENT_SECRET).orElse(null);
        tokenEndpointUrl = EnvironmentUtils.getOptionalProperty(AZURE_OPENID_CONFIG_TOKEN_ENDPOINT).orElse(null);

        return this;
    }

    public AzureAdServiceTokenProviderBuilder withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public AzureAdServiceTokenProviderBuilder withClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public AzureAdServiceTokenProviderBuilder withTokenEndpointUrl(String tokenEndpointUrl) {
        this.tokenEndpointUrl = tokenEndpointUrl;
        return this;
    }

    public AzureAdServiceTokenProvider build() {
        if (clientId == null) {
            throw new IllegalStateException("Client ID is required");
        }

        if (clientSecret == null) {
            throw new IllegalStateException("Client secret is required");
        }

        if (tokenEndpointUrl == null) {
            throw new IllegalStateException("Token endpoint URL is required");
        }

        ScopedTokenProvider scopedTokenProvider = new AzureAdScopedTokenProvider(clientId, clientSecret, tokenEndpointUrl);

        return new AzureAdServiceTokenProvider(new CachedScopedTokenProvider(scopedTokenProvider));
    }

}
