package no.nav.common.aktorregisterklient;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CachedAktorregisterKlient implements AktorregisterKlient {

    private final AktorregisterKlient aktorregisterKlient;

    private final Cache<String, String> hentFnrCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(10_000)
            .build();

    private final Cache<String, String> hentAktorIdCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(10_000)
            .build();

    public CachedAktorregisterKlient(AktorregisterKlient aktorregisterKlient) {
        this.aktorregisterKlient = aktorregisterKlient;
    }

    @Override
    public String hentFnr(String aktorId) {
        String cachedFnr = hentFnrCache.getIfPresent(aktorId);

        if (cachedFnr == null) {
            String fnr = aktorregisterKlient.hentFnr(aktorId);
            hentFnrCache.put(aktorId, fnr);
            return fnr;
        }

        return cachedFnr;
    }

    @Override
    public String hentAktorId(String fnr) {
        String cachedAktorId = hentAktorIdCache.getIfPresent(fnr);

        if (cachedAktorId == null) {
            String aktorId = aktorregisterKlient.hentAktorId(fnr);
            hentAktorIdCache.put(fnr, aktorId);
            return aktorId;
        }

        return cachedAktorId;
    }

    @Override
    public List<IdentOppslag> hentFnr(List<String> aktorIdListe) {
        return aktorregisterKlient.hentFnr(aktorIdListe);
    }

    @Override
    public List<IdentOppslag> hentAktorId(List<String> fnrListe) {
        return aktorregisterKlient.hentAktorId(fnrListe);
    }
}
