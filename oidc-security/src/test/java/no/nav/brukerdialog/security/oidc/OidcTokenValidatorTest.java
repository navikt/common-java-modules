package no.nav.brukerdialog.security.oidc;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.jwks.CacheMissAction;
import no.nav.brukerdialog.security.jwks.JwtHeader;
import no.nav.brukerdialog.security.oidc.provider.OidcProvider;
import no.nav.json.JsonUtils;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OidcTokenValidatorTest {

    private static final String VALID_AUDIENCE = "valid audience";
    private static final String VALID_ISSUER = "valid issuer";
    private static final RsaJsonWebKey RSA_JSON_WEB_KEY = createKey();

    private TestToken tokenWithValidAudience;
    private TestToken tokenWithInvalidAudience;
    private TestToken tokenWithoutAudience;
    private TestToken tokenWithInvalidIssuer;

    private OidcTokenValidator oidcTokenValidator = new OidcTokenValidator();

    @Before
    public void setup() {
        tokenWithValidAudience = generate(VALID_AUDIENCE, VALID_ISSUER);
        tokenWithInvalidAudience = generate("invalid audience", VALID_ISSUER);
        tokenWithoutAudience = generate(null, VALID_ISSUER);
        tokenWithInvalidIssuer = generate(VALID_AUDIENCE, "invalid issuer");
    }

    @Test
    public void validate__valid_audience_and_issuer__valid() {
        assertThat(validate(tokenWithValidAudience, provider(VALID_AUDIENCE))).satisfies(this::isValid);
    }

    @Test
    public void validate__invalid_audience__invalid() {
        assertThat(validate(tokenWithInvalidAudience, provider(VALID_AUDIENCE)).getErrorMessage()).contains("aud");
    }

    @Test
    public void validate__no_audience__invalid() {
        assertThat(validate(tokenWithoutAudience, provider(VALID_AUDIENCE)).getErrorMessage()).contains("aud");
    }

    @Test
    public void validate__audience_not_expected__any_audience_valid() {
        assertThat(validate(tokenWithValidAudience, provider(null))).satisfies(this::isValid);
        assertThat(validate(tokenWithInvalidAudience, provider(null))).satisfies(this::isValid);
        assertThat(validate(tokenWithoutAudience, provider(null))).satisfies(this::isValid);
    }

    @Test
    public void validate__invalid_issuer__invalid() {
        assertThat(validate(tokenWithInvalidIssuer, provider(VALID_AUDIENCE)).getErrorMessage()).contains("iss");
    }

    private OidcTokenValidatorResult validate(TestToken testToken, OidcProvider oidcProvider) {
        return oidcTokenValidator.validate(testToken.token, oidcProvider);
    }

    private void isValid(OidcTokenValidatorResult oidcTokenValidatorResult) {
        assertThat(oidcTokenValidatorResult.isValid())
                .describedAs(JsonUtils.toJson(oidcTokenValidatorResult))
                .isTrue();
    }

    private OidcProvider provider(final String expectedAudience) {
        return new OidcProvider() {
            @Override
            public Optional<String> getToken(HttpServletRequest httpServletRequest) {
                return Optional.empty();
            }

            @Override
            public Optional<String> getRefreshToken(HttpServletRequest httpServletRequest) {
                return Optional.empty();
            }

            @Override
            public OidcCredential getFreshToken(String refreshToken, String requestToken) {
                return null;
            }

            @Override
            public Optional<Key> getVerificationKey(JwtHeader header, CacheMissAction cacheMissAction) {
                return Optional.of(RSA_JSON_WEB_KEY.getRsaPublicKey());
            }

            @Override
            public String getExpectedIssuer() {
                return OidcTokenValidatorTest.VALID_ISSUER;
            }

            @Override
            public String getExpectedAudience(String token) {
                return expectedAudience;
            }

            @Override
            public IdentType getIdentType(String token) {
                return null;
            }
        };
    }


    @SneakyThrows
    private static TestToken generate(String aud, String issuer) {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(issuer);
        claims.setAudience(aud);
        claims.setExpirationTimeMinutesInTheFuture(120);
        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setNotBeforeMinutesInThePast(2);
        claims.setSubject("12345678901");
        JsonWebSignature jws = new JsonWebSignature();

        jws.setPayload(claims.toJson());
        jws.setKey(RSA_JSON_WEB_KEY.getPrivateKey());
        jws.setKeyIdHeaderValue(RSA_JSON_WEB_KEY.getKeyId());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        return TestToken.builder()
                .issuer(issuer)
                .token(jws.getCompactSerialization())
                .key(RSA_JSON_WEB_KEY)
                .build();
    }

    @SneakyThrows
    private static RsaJsonWebKey createKey() {
        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        rsaJsonWebKey.setKeyId(UUID.randomUUID().toString());
        rsaJsonWebKey.setUse("sig");
        rsaJsonWebKey.setAlgorithm("RS256");
        return rsaJsonWebKey;
    }

    @Builder
    @Value
    private static class TestToken {
        public String token;
        public String issuer;
        public RsaJsonWebKey key;
    }

}