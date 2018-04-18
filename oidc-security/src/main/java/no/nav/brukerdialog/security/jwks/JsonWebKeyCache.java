package no.nav.brukerdialog.security.jwks;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.security.Key;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.sbl.rest.RestUtils.withClient;
import static no.nav.sbl.util.StringUtils.assertNotNullOrEmpty;

public class JsonWebKeyCache {

    private static final Logger log = LoggerFactory.getLogger(JsonWebKeyCache.class);

    private final String jwksUrl;
    private final boolean expectAlgorithmInKey;

    private JsonWebKeySet keyCache;

    public JsonWebKeyCache(String jwksUrl, boolean expectAlgorithmInKey) {
        this.jwksUrl = assertNotNullOrEmpty(jwksUrl);
        this.expectAlgorithmInKey = expectAlgorithmInKey;
    }

    public synchronized Key getVerificationKey(JwtHeader header) {
        Key key = getCachedKey(header);
        if (key != null) {
            return key;
        }
        refreshKeyCache();
        return getCachedKey(header);
    }

    private Key getCachedKey(JwtHeader header) {
        if (keyCache == null) {
            return null;
        }
        JsonWebKey jwk = keyCache.findJsonWebKey(header.getKid(), "RSA", "sig", expectAlgorithmInKey ? header.getAlgorithm() : null);
        if (jwk == null) {
            return null;
        }
        return jwk.getKey();
    }

    private void setKeyCache(String jwksAsString) {
        try {
            keyCache = new JsonWebKeySet(jwksAsString);
        } catch (JoseException e) {
            log.error("Could not parse JWKs.");
        }
    }

    private void refreshKeyCache() {
        keyCache = null;
        try {
            String jwksString = httpGet(jwksUrl);
            setKeyCache(jwksString);
            log.info("JWKs cache updated with: " + jwksString);
        } catch (RuntimeException e) {
            log.error("JWKs cache update failed. ", e);
        }
    }

    private static String httpGet(String url) {
        if (url == null) {
            throw new IllegalArgumentException("Missing URL to JWKs location");
        }
        log.info("Starting JWKS update from " + url);
        return withClient(client -> {
            Response response = client.target(url)
                    .request()
                    .header(ACCEPT, APPLICATION_JSON)
                    .get();

            int responseStatus = response.getStatus();
            if (responseStatus != 200) {
                String error = "jwks cache update failed : HTTP error code : " + responseStatus;
                log.error(error);
                throw new RuntimeException(error);
            }
            return response.readEntity(String.class);
        });
    }

}
