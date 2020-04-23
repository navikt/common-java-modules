package no.nav.common.aktorregisterklient;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.List;
import java.util.Optional;
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
    public Optional<String> hentFnr(String aktorId) {
        String cachedFnr = hentFnrCache.getIfPresent(aktorId);

        if (cachedFnr == null) {
            Optional<String> maybeFnr = aktorregisterKlient.hentFnr(aktorId);
            maybeFnr.ifPresent(fnr -> hentFnrCache.put(aktorId, fnr));
            return maybeFnr;
        }

        return Optional.of(cachedFnr);
    }

    @Override
    public Optional<String> hentAktorId(String fnr) {
        String cachedAktorId = hentAktorIdCache.getIfPresent(fnr);

        if (cachedAktorId == null) {
            Optional<String> maybeAktorId = aktorregisterKlient.hentAktorId(fnr);
            maybeAktorId.ifPresent(aktorId -> hentAktorIdCache.put(fnr, aktorId));
            return maybeAktorId;
        }

        return Optional.of(cachedAktorId);
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
