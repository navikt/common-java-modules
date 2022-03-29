package no.nav.common.token_client.builder;

import no.nav.common.token_client.cache.CaffeineTokenCache;
import no.nav.common.token_client.cache.TokenCache;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;

import static no.nav.common.token_client.utils.env.AzureAdEnvironmentVariables.*;

public class AzureAdTokenClientBuilder {

    private String clientId;

    private String privateJwk;

    private String tokenEndpointUrl;

    private TokenCache tokenCache = new CaffeineTokenCache();

    private AzureAdTokenClientBuilder() {}

    public static AzureAdTokenClientBuilder builder() {
        return new AzureAdTokenClientBuilder();
    }

    public AzureAdTokenClientBuilder withNaisDefaults() {
        clientId = System.getenv(AZURE_APP_CLIENT_ID);
        privateJwk = System.getenv(AZURE_APP_JWK);
        tokenEndpointUrl = System.getenv(AZURE_OPENID_CONFIG_TOKEN_ENDPOINT);

        return this;
    }

    public AzureAdTokenClientBuilder withCache(TokenCache tokenCache) {
        this.tokenCache = tokenCache;
        return this;
    }

    public AzureAdTokenClientBuilder withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public AzureAdTokenClientBuilder withPrivateJwk(String privateJwk) {
        this.privateJwk = privateJwk;
        return this;
    }

    public AzureAdTokenClientBuilder withTokenEndpointUrl(String tokenEndpointUrl) {
        this.tokenEndpointUrl = tokenEndpointUrl;
        return this;
    }

    public AzureAdMachineToMachineTokenClient buildMachineToMachineTokenClient() {
        validate();
        return new AzureAdMachineToMachineTokenClient(clientId, tokenEndpointUrl, privateJwk, tokenCache);
    }

    public AzureAdOnBehalfOfTokenClient buildOnBehalfOfTokenClient() {
        validate();
        return new AzureAdOnBehalfOfTokenClient(clientId, tokenEndpointUrl, privateJwk, tokenCache);
    }

    private void validate() {
        if (clientId == null) {
            throw new IllegalStateException("Client ID is required");
        }

        if (privateJwk == null) {
            throw new IllegalStateException("Private JWK is required");
        }

        if (tokenEndpointUrl == null) {
            throw new IllegalStateException("Token endpoint URL is required");
        }
    }

}
