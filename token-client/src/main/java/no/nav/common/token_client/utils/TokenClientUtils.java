package no.nav.common.token_client.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TokenClientUtils {

    public static PrivateKeyJWT signedClientAssertion(JWSHeader assertionHeader, JWTClaimsSet assertionClaims, JWSSigner signer) throws JOSEException {
        SignedJWT signedJWT = new SignedJWT(assertionHeader, assertionClaims);
        signedJWT.sign(signer);
        return new PrivateKeyJWT(signedJWT);
    }

    public static JWSHeader clientAssertionHeader(String keyId) throws ParseException {
        Map<String, Object> headerClaims = new HashMap<>();
        headerClaims.put("kid", keyId);
        headerClaims.put("typ", "JWT");
        headerClaims.put("alg", "RS256");

        return JWSHeader.parse(headerClaims);
    }

    public static JWTClaimsSet clientAssertionClaims(String clientId, String audience) {
        Date now = new Date();
        Date expiration = new Date(now.toInstant().plusSeconds(30).toEpochMilli());

        return new JWTClaimsSet.Builder()
                .subject(clientId)
                .issuer(clientId)
                .audience(audience)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(now)
                .notBeforeTime(now)
                .expirationTime(expiration)
                .build();
    }

}
