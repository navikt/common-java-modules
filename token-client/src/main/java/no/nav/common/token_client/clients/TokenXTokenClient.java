package no.nav.common.token_client.clients;

import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.TokenTypeURI;
import com.nimbusds.oauth2.sdk.tokenexchange.TokenExchangeGrant;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.token_client.OnBehalfOfTokenClient;
import no.nav.common.token_client.utils.OidcDiscoveryClient;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.nav.common.token_client.utils.TokenClientUtils.*;

@Slf4j
public class TokenXTokenClient implements OnBehalfOfTokenClient {

    private final String clientId;

    private final URI tokenEndpoint;

    private final String privateJwkKeyId;

    private final JWSSigner assertionSigner;

    @SneakyThrows
    public TokenXTokenClient(String clientId, String privateJwk, String discoveryUrl) {
        this.clientId = clientId;

        RSAKey rsaKey = RSAKey.parse(privateJwk);
        privateJwkKeyId = rsaKey.getKeyID();
        assertionSigner = new RSASSASigner(rsaKey);

        OIDCProviderMetadata oidcProviderMetadata = OidcDiscoveryClient.fetchDiscoveryMetadata(discoveryUrl);
        tokenEndpoint = oidcProviderMetadata.getTokenEndpointURI();
    }

    @SneakyThrows
    @Override
    public String exchangeOnBehalfOfToken(String appIdentifier, String accessToken) {
        PrivateKeyJWT signedJwt = signedClientAssertion(
                clientAssertionHeader(privateJwkKeyId),
                clientAssertionClaims(clientId, tokenEndpoint.toString()),
                assertionSigner
        );

        TokenRequest request = new TokenRequest(
                tokenEndpoint,
                signedJwt,
                new TokenExchangeGrant(new BearerAccessToken(accessToken), TokenTypeURI.ACCESS_TOKEN),
                new Scope(appIdentifier),
                null,
                additionalClaims(appIdentifier, accessToken)
        );

        TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());

        if (!response.indicatesSuccess()) {
            TokenErrorResponse tokenErrorResponse = response.toErrorResponse();
            log.error("Failed to fetch TokenX OBO for scope={}. Error: {}", appIdentifier, tokenErrorResponse.toJSONObject().toString());
            throw new RuntimeException("Failed to fetch TokenX OBO token for scope=" + appIdentifier);
        }

        AccessTokenResponse successResponse = response.toSuccessResponse();

        return successResponse.getTokens().getAccessToken().getValue();
    }

    private static Map<String, List<String>> additionalClaims(String audience, String accessToken) {
        Map<String, List<String>> customParams = new HashMap<>();
        customParams.put("audience", List.of(audience));
        customParams.put("subject_token", List.of(accessToken));
        customParams.put("subject_token_type", List.of("urn:ietf:params:oauth:token-type:jwt"));

        return customParams;
    }

}