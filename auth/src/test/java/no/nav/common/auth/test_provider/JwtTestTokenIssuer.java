package no.nav.common.auth.test_provider;

import lombok.SneakyThrows;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.ReservedClaimNames;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static no.nav.common.utils.AssertUtils.assertNotNull;

public class JwtTestTokenIssuer {

    public final RsaJsonWebKey key;
    public final String issuer, audience;

    public JwtTestTokenIssuer(JwtTestTokenIssuerConfig config) {
        issuer = config.issuer;
        audience = config.audience;
        key = generateKey(config);
    }

    @SneakyThrows
    private static RsaJsonWebKey generateKey(JwtTestTokenIssuerConfig config) {
        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        rsaJsonWebKey.setKeyId(config.id);
        rsaJsonWebKey.setUse("sig");
        rsaJsonWebKey.setAlgorithm(config.algorithm);

        return rsaJsonWebKey;
    }

    public String getKeySetJson() {
        return new JsonWebKeySet(Collections.singletonList(key)).toJson();
    }

    @SneakyThrows
    public String issueTestToken(Claims claims) {
        // https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setIssuer(issuer);
        jwtClaims.setAudience(audience);
        jwtClaims.setExpirationTimeMinutesInTheFuture(120);
        jwtClaims.setGeneratedJwtId();
        jwtClaims.setIssuedAtToNow();
        jwtClaims.setNotBeforeMinutesInThePast(2);

        claims.map.forEach(jwtClaims::setClaim);

        JsonWebSignature jws = new JsonWebSignature();

        jws.setPayload(jwtClaims.toJson());
        jws.setKey(key.getPrivateKey());
        jws.setKeyIdHeaderValue(key.getKeyId());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        return jws.getCompactSerialization();
    }

    public static class Claims {

        private final Map<String, Object> map = new HashMap<>();

        public Claims(String subject) {
            map.put(ReservedClaimNames.SUBJECT, assertNotNull(subject));
        }

        public Claims setClaim(String name, Object value) {
            map.put(name, value);
            return this;
        }

    }
}
