package no.nav.common.client.pdl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;

public class CachedAktorOppslagClient implements AktorOppslagClient {

    private final AktorOppslagClient aktorOppslagClient;

    private final Cache<AktorId, Fnr> hentFnrCache;

    private final Cache<Fnr, AktorId> hentAktorIdCache;

    public CachedAktorOppslagClient(AktorOppslagClient aktorOppslagClient, Cache<AktorId, Fnr> hentFnrCache, Cache<Fnr, AktorId> hentAktorIdCache) {
        this.aktorOppslagClient = aktorOppslagClient;
        this.hentFnrCache = hentFnrCache;
        this.hentAktorIdCache = hentAktorIdCache;
    }

    public CachedAktorOppslagClient(AktorOppslagClient aktorOppslagClient) {
        this.aktorOppslagClient = aktorOppslagClient;

        hentFnrCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .maximumSize(10_000)
                .build();

        hentAktorIdCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .maximumSize(10_000)
                .build();
    }

    @Override
    public Fnr hentFnr(AktorId aktorId) {
        return tryCacheFirst(hentFnrCache, aktorId, () -> aktorOppslagClient.hentFnr(aktorId));
    }

    @Override
    public AktorId hentAktorId(Fnr fnr) {
        return tryCacheFirst(hentAktorIdCache, fnr, () -> aktorOppslagClient.hentAktorId(fnr));
    }

    @Override
    public Map<AktorId, Fnr> hentFnrBolk(List<AktorId> aktorIdListe) {
        Map<AktorId, Fnr> cachedMapping = new HashMap<>();
        List<AktorId> uncachedAktorIds = new ArrayList<>();

        aktorIdListe.forEach(aktorId -> {
            Fnr fnr = hentFnrCache.getIfPresent(aktorId);
            if (fnr != null) {
                cachedMapping.put(aktorId, fnr);
            } else {
                uncachedAktorIds.add(aktorId);
            }
        });

        Map<AktorId, Fnr> identOppslag = aktorOppslagClient.hentFnrBolk(uncachedAktorIds);

        // Update cache
        identOppslag.forEach(hentFnrCache::put);

        // Add from previously cached results
        identOppslag.putAll(cachedMapping);

        return identOppslag;
    }

    @Override
    public Map<Fnr, AktorId> hentAktorIdBolk(List<Fnr> fnrListe) {
        Map<Fnr, AktorId> cachedMapping = new HashMap<>();
        List<Fnr> uncachedFnrs = new ArrayList<>();

        fnrListe.forEach(fnr -> {
            AktorId aktorId = hentAktorIdCache.getIfPresent(fnr);
            if (aktorId != null) {
                cachedMapping.put(fnr, aktorId);
            } else {
                uncachedFnrs.add(fnr);
            }
        });

        Map<Fnr, AktorId> identOppslag = aktorOppslagClient.hentAktorIdBolk(uncachedFnrs);

        // Update cache
        identOppslag.forEach(hentAktorIdCache::put);

        // Add from previously cached results
        identOppslag.putAll(cachedMapping);

        return identOppslag;
    }

    @Override
    public HealthCheckResult checkHealth() {
        return aktorOppslagClient.checkHealth();
    }

}
