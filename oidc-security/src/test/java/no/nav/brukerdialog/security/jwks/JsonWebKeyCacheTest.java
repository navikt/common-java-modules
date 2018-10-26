package no.nav.brukerdialog.security.jwks;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.SneakyThrows;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.security.Key;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.brukerdialog.security.jwks.CacheMissAction.NO_REFRESH;
import static no.nav.brukerdialog.security.jwks.CacheMissAction.REFRESH;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonWebKeyCacheTest {

    private static final String KID = "kid";
    private static final String ALGORITHM = "RS256";

    private JwtHeader jwtHeader = new JwtHeader(KID, ALGORITHM);

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Before
    @SneakyThrows
    public void setup() {
        JsonWebKeySet jsonWebKeySet = new JsonWebKeySet();
        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        rsaJsonWebKey.setKeyId(KID);
        rsaJsonWebKey.setUse("sig");
        rsaJsonWebKey.setAlgorithm(ALGORITHM);
        jsonWebKeySet.addJsonWebKey(rsaJsonWebKey);
        String body = jsonWebKeySet.toJson();

        givenThat(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(body)
                )
        );
    }

    @Test
    public void getVerificationKey__refresh__return_matching_key() {
        JsonWebKeyCache jsonWebKeyCache = buildCache();

        assertThat(jsonWebKeyCache.getVerificationKey(jwtHeader, REFRESH)).isNotNull();

        assertThat(jsonWebKeyCache.getVerificationKey(jwtHeader, NO_REFRESH)).isNotNull();
        assertThat(jsonWebKeyCache.getVerificationKey(jwtHeader, NO_REFRESH)).isNotNull();
        assertThat(jsonWebKeyCache.getVerificationKey(jwtHeader, REFRESH)).isNotNull();
        assertThat(jsonWebKeyCache.getVerificationKey(jwtHeader, REFRESH)).isNotNull();

        verify(1, getRequestedFor(urlEqualTo("/")));
    }

    @Test
    public void getVerificationKey__no_refresh__no_matching_key_available() {
        JsonWebKeyCache jsonWebKeyCache = buildCache();

        Optional<Key> verificationKey = jsonWebKeyCache.getVerificationKey(jwtHeader, NO_REFRESH);

        assertThat(verificationKey).isEmpty();
        verify(0, getRequestedFor(urlEqualTo("/")));
    }

    @Test
    public void getVerificationKey__key_mismatch__refresh_cache_if_requested() {
        JsonWebKeyCache jsonWebKeyCache = buildCache();
        assertThat(jsonWebKeyCache.getVerificationKey(new JwtHeader(UUID.randomUUID().toString(), ALGORITHM), NO_REFRESH)).isEmpty();
        assertThat(jsonWebKeyCache.getVerificationKey(new JwtHeader(UUID.randomUUID().toString(), ALGORITHM), NO_REFRESH)).isEmpty();

        assertThat(jsonWebKeyCache.getVerificationKey(new JwtHeader(UUID.randomUUID().toString(), ALGORITHM), REFRESH)).isEmpty();
        assertThat(jsonWebKeyCache.getVerificationKey(new JwtHeader(UUID.randomUUID().toString(), ALGORITHM), REFRESH)).isEmpty();
        assertThat(jsonWebKeyCache.getVerificationKey(new JwtHeader(UUID.randomUUID().toString(), ALGORITHM), REFRESH)).isEmpty();

        verify(3, getRequestedFor(urlEqualTo("/")));
    }

    @Test
    public void getVerificationKey__jwks_not_available__no_key_available() {
        JsonWebKeyCache jsonWebKeyCache = new JsonWebKeyCache("http://localhost:12345", false);

        Optional<Key> verificationKey = jsonWebKeyCache.getVerificationKey(jwtHeader, REFRESH);

        assertThat(verificationKey).isEmpty();
    }

    @Test(timeout = 20_000L)
    public void getVerificationKey__jwks_endpoint_slow__timeout_and_no_key_available() {
        givenThat(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(40_000)
                )
        );
        JsonWebKeyCache jsonWebKeyCache = buildCache();
        assertThat(jsonWebKeyCache.getVerificationKey(jwtHeader, REFRESH)).isEmpty();
    }

    private JsonWebKeyCache buildCache() {
        return new JsonWebKeyCache("http://localhost:" + wireMockRule.port(), false);
    }

}