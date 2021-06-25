package no.nav.common.client.aktoroppslag;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EksternBrukerId;
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

    private final Cache<EksternBrukerId, BrukerIdenter> hentIdenterCache;

    public CachedAktorOppslagClient(
            AktorOppslagClient aktorOppslagClient,
            Cache<AktorId, Fnr> hentFnrCache,
            Cache<Fnr, AktorId> hentAktorIdCache,
            Cache<EksternBrukerId, BrukerIdenter> hentIdenterCache
    ) {
        this.aktorOppslagClient = aktorOppslagClient;
        this.hentFnrCache = hentFnrCache;
        this.hentAktorIdCache = hentAktorIdCache;
        this.hentIdenterCache = hentIdenterCache;
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

        hentIdenterCache = Caffeine.newBuilder()
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
        Map<AktorId, Fnr> aktorIdTilFnrMap = new HashMap<>();
        List<AktorId> uncachedAktorIds = new ArrayList<>();

        aktorIdListe.forEach(aktorId -> {
            Fnr fnr = hentFnrCache.getIfPresent(aktorId);
            if (fnr != null) {
                aktorIdTilFnrMap.put(aktorId, fnr);
            } else {
                uncachedAktorIds.add(aktorId);
            }
        });

        if (!uncachedAktorIds.isEmpty()) {
            Map<AktorId, Fnr> identOppslag = aktorOppslagClient.hentFnrBolk(uncachedAktorIds);

            identOppslag.forEach(hentFnrCache::put);

            aktorIdTilFnrMap.putAll(identOppslag);
        }

        return aktorIdTilFnrMap;
    }

    @Override
    public Map<Fnr, AktorId> hentAktorIdBolk(List<Fnr> fnrListe) {
        Map<Fnr, AktorId> fnrTilAktorIdMap = new HashMap<>();
        List<Fnr> uncachedFnrs = new ArrayList<>();

        fnrListe.forEach(fnr -> {
            AktorId aktorId = hentAktorIdCache.getIfPresent(fnr);
            if (aktorId != null) {
                fnrTilAktorIdMap.put(fnr, aktorId);
            } else {
                uncachedFnrs.add(fnr);
            }
        });

        if (!uncachedFnrs.isEmpty()) {
            Map<Fnr, AktorId> identOppslag = aktorOppslagClient.hentAktorIdBolk(uncachedFnrs);

            identOppslag.forEach(hentAktorIdCache::put);

            fnrTilAktorIdMap.putAll(identOppslag);
        }

        return fnrTilAktorIdMap;
    }

    @Override
    public BrukerIdenter hentIdenter(EksternBrukerId brukerId) {
        return tryCacheFirst(hentIdenterCache, brukerId, () -> aktorOppslagClient.hentIdenter(brukerId));
    }

    @Override
    public HealthCheckResult checkHealth() {
        return aktorOppslagClient.checkHealth();
    }

}
