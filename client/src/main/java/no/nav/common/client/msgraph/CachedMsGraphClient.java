package no.nav.common.client.msgraph;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nimbusds.jwt.JWTParser;
import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;

import java.util.concurrent.TimeUnit;

import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;

public class CachedMsGraphClient implements MsGraphClient {

    private final MsGraphClient msGraphClient;

    private final Cache<String, UserData> hentUserDataCache = Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    private final Cache<String, String> hentOnPremisesSamAccountNameCache = Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(20_000)
            .build();

    public CachedMsGraphClient(MsGraphClient msGraphClient) {
        this.msGraphClient = msGraphClient;
    }

    @SneakyThrows
    @Override
    public UserData hentUserData(String userAccessToken) {
        String cacheKey = JWTParser.parse(userAccessToken).getJWTClaimsSet().getSubject();
        return tryCacheFirst(hentUserDataCache, cacheKey, () -> msGraphClient.hentUserData(userAccessToken));
    }

    @SneakyThrows
    @Override
    public String hentOnPremisesSamAccountName(String userAccessToken) {
        String cacheKey = JWTParser.parse(userAccessToken).getJWTClaimsSet().getSubject();
        return tryCacheFirst(hentOnPremisesSamAccountNameCache, cacheKey, () -> msGraphClient.hentOnPremisesSamAccountName(userAccessToken));
    }

    @Override
    public HealthCheckResult checkHealth() {
        return msGraphClient.checkHealth();
    }

}
