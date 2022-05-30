package no.nav.common.token_client.test_utils;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;

import java.util.Date;
import java.util.UUID;

public class TokenCreator {

    private static TokenCreator tokenCreator;

    private final RSAKey rsaJWK;

    private final JWSSigner signer;

    @SneakyThrows
    private TokenCreator() {
        rsaJWK = new RSAKeyGenerator(2048)
                .keyID(UUID.randomUUID().toString())
                .generate();

        signer = new RSASSASigner(rsaJWK);
    }

    @SneakyThrows
    public String createToken() {
        return createToken(60 * 1000);
    }

    @SneakyThrows
    public String createToken(int expireFromNowMs) {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("test")
                .issuer("https://example.com")
                .jwtID(UUID.randomUUID().toString())
                .expirationTime(new Date(new Date().getTime() + expireFromNowMs))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(),
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
