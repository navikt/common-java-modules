package no.nav.common.abac;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.SneakyThrows;
import no.nav.common.abac.domain.response.XacmlResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AbacCache {

    private final Cache<String, XacmlResponse> abacCache = Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(20_000)
            .build();


    public Optional<XacmlResponse> findResponse(String requestJson) {
        return Optional.ofNullable(abacCache.getIfPresent(createCacheKey(requestJson)));
    }

    public void cacheResponse(String requestJson, XacmlResponse xacmlResponse) {
        abacCache.put(createCacheKey(requestJson), xacmlResponse);
    }

    private static String createCacheKey(String requestJson) {
        byte[] sha1Hash = createSha1Hash(requestJson);
        return bytesToHex(sha1Hash);
    }

    @SneakyThrows
    private static byte[] createSha1Hash(String text) {
        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(text.getBytes(StandardCharsets.UTF_8));
        return crypt.digest();
    }

    private static String bytesToHex(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

}
