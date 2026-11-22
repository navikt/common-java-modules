package no.nav.common.token_client.test_utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.util.Date;
import java.util.UUID;
import lombok.SneakyThrows;

public class TokenCreator {

    private static TokenCreator tokenCreator;

    private final RSAKey rsaJWK;

    private final JWSSigner signer;

    // Opprettelse skjer via instance(), som brukes i Supplier-callbacker.
    @SneakyThrows(JOSEException.class)
    private TokenCreator() {
        rsaJWK = new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate();

        signer = new RSASSASigner(rsaJWK);
    }

    public String createToken(String subject) {
        return createToken(subject, 60 * 1000);
    }

    // Supplier-callbackene som bruker metoden, tillater ikke checked exceptions.
    @SneakyThrows(JOSEException.class)
    public String createToken(String subject, int expireFromNowMs) {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer("https://example.com")
                .jwtID(UUID.randomUUID().toString())
                .expirationTime(new Date(new Date().getTime() + expireFromNowMs))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaJWK.getKeyID())
                        .build(),
                claimsSet);

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public static TokenCreator instance() {
        if (tokenCreator == null) {
            tokenCreator = new TokenCreator();
        }

        return tokenCreator;
    }
}
