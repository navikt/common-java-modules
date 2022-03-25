package no.nav.common.token_client.builder;

import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import no.nav.common.token_client.cache.CaffeineTokenCache;
import no.nav.common.token_client.cache.TokenCache;
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient;
import no.nav.common.token_client.utils.OidcDiscoveryClient;

import static no.nav.common.token_client.utils.env.TokenXEnvironmentvariables.*;

public class TokenXTokenClientBuilder {

    private String clientId;

    private String privateJwk;

    private String discoveryUrl;

    private TokenCache tokenCache = new CaffeineTokenCache();

    private TokenXTokenClientBuilder() {}

    public static TokenXTokenClientBuilder builder() {
        return new TokenXTokenClientBuilder();
    }

    public TokenXTokenClientBuilder withNaisDefaults() {
        clientId = System.getenv(TOKEN_X_CLIENT_ID);
        privateJwk = System.getenv(TOKEN_X_PRIVATE_JWK);
        discoveryUrl = System.getenv(TOKEN_X_WELL_KNOWN_URL);

        return this;
    }

    public TokenXTokenClientBuilder withCache(TokenCache tokenCache) {
        this.tokenCache = tokenCache;
        return this;
    }

    public TokenXTokenClientBuilder withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public TokenXTokenClientBuilder withPrivateJwk(String privateJwk) {
        this.privateJwk = privateJwk;
        return this;
    }

    public TokenXTokenClientBuilder withDiscoveryUrl(String discoveryUrl) {
        this.discoveryUrl = discoveryUrl;
        return this;
    }

    public TokenXOnBehalfOfTokenClient buildOnBehalfOfTokenClient() {
        validate();

        OIDCProviderMetadata oidcProviderMetadata = OidcDiscoveryClient.fetchDiscoveryMetadata(discoveryUrl);

        return new TokenXOnBehalfOfTokenClient(clientId, oidcProviderMetadata.getTokenEndpointURI().toString(), privateJwk, tokenCache);
    }

    private void validate() {
        if (clientId == null) {
            throw new IllegalStateException("Client ID is required");
        }

        if (privateJwk == null) {
            throw new IllegalStateException("Private JWK is required");
        }

        if (discoveryUrl == null) {
            throw new IllegalStateException("Discovery URL is required");
        }
    }

}
