package no.nav.brukerdialog.security.jwks;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.Key;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JwksKeyHandlerImpl implements JwksKeyHandler {

    private static final Logger log = LoggerFactory.getLogger(JwksKeyHandlerImpl.class);

    private final Supplier<String> jwksStringSupplier;
    private JsonWebKeySet keyCache;


    JwksKeyHandlerImpl() {
        this(() -> httpGet(System.getProperty("isso-jwks.url")));
    }

    public JwksKeyHandlerImpl(Supplier<String> jwksStringSupplier) {
        this.jwksStringSupplier = jwksStringSupplier;
    }

    @Override
    public synchronized Key getKey(JwtHeader header) {
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
        JsonWebKey jwk = keyCache.findJsonWebKey(header.getKid(), "RSA", "sig", header.getAlgorithm());
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
            String jwksString = jwksStringSupplier.get();
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
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("accept", "application/json");
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    String error = "jwks cache update failed : HTTP error code : " + response.getStatusLine().getStatusCode();
                    log.error(error);
                    throw new RuntimeException(error);
                }
                return new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
                        .lines()
                        .collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
