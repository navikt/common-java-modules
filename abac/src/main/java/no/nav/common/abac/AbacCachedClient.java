package no.nav.common.abac;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.SneakyThrows;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.XacmlResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AbacCachedClient implements AbacClient {

    private final Cache<String, String> abacCache;

    private final AbacClient abacClient;

    public AbacCachedClient(AbacClient abacClient) {
        this.abacClient = abacClient;
        this.abacCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .maximumSize(20_000)
                .build();
    }

    public AbacCachedClient(AbacClient abacClient, Cache<String, String> abacCache) {
        this.abacClient = abacClient;
        this.abacCache = abacCache;
    }

    @Override
    public XacmlResponse sendRequest(XacmlRequest xacmlRequest) {
        String xacmlRequestJson = XacmlMapper.mapRequestToEntity(xacmlRequest);
        String xacmlResponseJson = sendRawRequest(xacmlRequestJson);
        return XacmlMapper.mapRawResponse(xacmlResponseJson);
    }

    @Override
    public String sendRawRequest(String xacmlRequestJson) {
        String cacheKey = createCacheKey(xacmlRequestJson);
        Optional<String> maybeCachedResponse = Optional.ofNullable(abacCache.getIfPresent(cacheKey));

        if (maybeCachedResponse.isPresent()) {
            return maybeCachedResponse.get();
        }

        String xacmleResponse = abacClient.sendRawRequest(xacmlRequestJson);
        abacCache.put(cacheKey, xacmleResponse);

        return xacmleResponse;
    }

    private static String createCacheKey(String requestJson) {
        byte[] sha1Hash = createSha1Hash(requestJson);
        return toBase64(sha1Hash);
    }

    @SneakyThrows
    private static byte[] createSha1Hash(String text) {
        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(text.getBytes(StandardCharsets.UTF_8));
        return crypt.digest();
    }

    private static String toBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

}
