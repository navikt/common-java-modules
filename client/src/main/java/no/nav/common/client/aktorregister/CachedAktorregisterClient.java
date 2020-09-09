package no.nav.common.client.aktorregister;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;

public class CachedAktorregisterClient implements AktorregisterClient {

    private final AktorregisterClient aktorregisterClient;

    private final Cache<String, Fnr> hentFnrCache = Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    private final Cache<String, AktorId> hentAktorIdCache = Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    public CachedAktorregisterClient(AktorregisterClient aktorregisterClient) {
        this.aktorregisterClient = aktorregisterClient;
    }

    @Override
    public Fnr hentFnr(AktorId aktorId) {
        return tryCacheFirst(hentFnrCache, aktorId.get(), () -> aktorregisterClient.hentFnr(aktorId));
    }

    @Override
    public AktorId hentAktorId(Fnr fnr) {
        return tryCacheFirst(hentAktorIdCache, fnr.get(), () -> aktorregisterClient.hentAktorId(fnr));
    }

    @Override
    public List<IdentOppslag> hentFnr(List<AktorId> aktorIdListe) {
        return aktorregisterClient.hentFnr(aktorIdListe);
    }

    @Override
    public List<IdentOppslag> hentAktorId(List<Fnr> fnrListe) {
        return aktorregisterClient.hentAktorId(fnrListe);
    }

    @Override
    public HealthCheckResult checkHealth() {
        return aktorregisterClient.checkHealth();
    }
}
