package no.nav.common.token_client.azure_ad;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.token_client.MachineToMachineTokenClient;
import no.nav.common.token_client.utils.OidcDiscoveryClient;

import java.net.URI;
import java.text.ParseException;
import java.util.*;

@Slf4j
public class AzureAdMachineToMachineTokenClient implements MachineToMachineTokenClient {

    private final String clientId;

    private final URI tokenEndpoint;

    private final String privateJwkKeyId;

    private final JWSSigner assertionSigner;

    @SneakyThrows
    public AzureAdMachineToMachineTokenClient(String clientId, String privateJwk, String discoveryUrl) {
        this.clientId = clientId;

        RSAKey rsaKey = RSAKey.parse(privateJwk);
        privateJwkKeyId = rsaKey.getKeyID();
        assertionSigner = new RSASSASigner(rsaKey);

        OIDCProviderMetadata oidcProviderMetadata = OidcDiscoveryClient.fetchDiscoveryMetadata(discoveryUrl);
        tokenEndpoint = oidcProviderMetadata.getTokenEndpointURI();
    }

    @SneakyThrows
    @Override
    public AccessToken createToken(String tokenScope) {
        SignedJWT signedJWT = new SignedJWT(
                clientAssertionHeader(privateJwkKeyId),
                clientAssertionClaims(clientId, tokenEndpoint.toString())
        );

        signedJWT.sign(assertionSigner);

        TokenRequest request = new TokenRequest(
                tokenEndpoint,
                new PrivateKeyJWT(signedJWT),
                new ClientCredentialsGrant(),
                new Scope(tokenScope),
                null,
                customClaims(tokenScope)
        );

        System.out.println(request.toHTTPRequest().getQuery());

        TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());

        if (!response.indicatesSuccess()) {
            TokenErrorResponse tokenErrorResponse = response.toErrorResponse();
            log.error("Failed to fetch AzureAD OBO token for scope={}. Error: {}", tokenScope, tokenErrorResponse.toJSONObject().toString());
            throw new RuntimeException("Failed to fetch AzureAD OBO token for scope=" + tokenScope);
        }

        AccessTokenResponse successResponse = response.toSuccessResponse();

        return successResponse.getTokens().getAccessToken();
    }

    private static Map<String, List<String>> customClaims(String audience) {
        Map<String, List<String>> customParams = new HashMap<>();
        customParams.put("audience", List.of(audience));

        return customParams;
    }

    private static JWSHeader clientAssertionHeader(String keyId) throws ParseException {
        Map<String, Object> headerClaims = new HashMap<>();
        headerClaims.put("kid", keyId);
        headerClaims.put("typ", "JWT");
        headerClaims.put("alg", "RS256");

        return JWSHeader.parse(headerClaims);
    }

    private static JWTClaimsSet clientAssertionClaims(String clientId, String tokenEndpointUrl) {
        Date now = new Date();
        Date expiration = new Date(now.toInstant().plusSeconds(30).toEpochMilli());

        return new JWTClaimsSet.Builder()
                .issuer(clientId)
                .subject(clientId)
                .audience(tokenEndpointUrl)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(now)
                .notBeforeTime(now)
                .expirationTime(expiration)
                .build();
    }


}
