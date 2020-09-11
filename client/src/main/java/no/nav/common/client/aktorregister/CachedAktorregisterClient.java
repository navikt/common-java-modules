package no.nav.common.client.aktorregister;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import no.nav.common.health.HealthCheckResult;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;

public class CachedAktorregisterClient implements AktorregisterClient {

    private final AktorregisterClient aktorregisterClient;

    private final Cache<String, String> hentFnrCache = Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    private final Cache<String, String> hentAktorIdCache = Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    public CachedAktorregisterClient(AktorregisterClient aktorregisterClient) {
        this.aktorregisterClient = aktorregisterClient;
    }

    @Override
    public String hentFnr(String aktorId) {
        return tryCacheFirst(hentFnrCache, aktorId, () -> aktorregisterClient.hentFnr(aktorId));
    }

    @Override
    public String hentAktorId(String fnr) {
        return tryCacheFirst(hentAktorIdCache, fnr, () -> aktorregisterClient.hentAktorId(fnr));
    }

    @Override
    public List<IdentOppslag> hentFnr(List<String> aktorIdListe) {
        return aktorregisterClient.hentFnr(aktorIdListe);
    }

    @Override
    public List<IdentOppslag> hentAktorId(List<String> fnrListe) {
        return aktorregisterClient.hentAktorId(fnrListe);
    }

    @Override
    public List<String> hentAktorIder(String fnr) {
        return aktorregisterClient.hentAktorIder(fnr);
    }

    @Override
    public HealthCheckResult checkHealth() {
        return aktorregisterClient.checkHealth();
    }
}
