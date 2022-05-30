package no.nav.common.token_client.client;

import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.SneakyThrows;
import no.nav.common.token_client.cache.TokenCache;

import java.net.URI;

public abstract class AbstractTokenClient {

    protected final String clientId;

    protected final URI tokenEndpoint;

    protected final String privateJwkKeyId;

    protected final JWSSigner assertionSigner;

    protected final TokenCache tokenCache;

    @SneakyThrows
    public AbstractTokenClient(String clientId, String tokenEndpointUrl, String privateJwk, TokenCache tokenCache) {
        this.clientId = clientId;
        this.tokenCache = tokenCache;

        tokenEndpoint = URI.create(tokenEndpointUrl);

        RSAKey rsaKey = RSAKey.parse(privateJwk);
        privateJwkKeyId = rsaKey.getKeyID();
        assertionSigner = new RSASSASigner(rsaKey);
    }

}
