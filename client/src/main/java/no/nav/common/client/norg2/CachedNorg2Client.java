package no.nav.common.client.norg2;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import no.nav.common.health.HealthCheckResult;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;
import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;

public class CachedNorg2Client implements Norg2Client {

    private final Norg2Client norg2Client;

    private final Cache<String, List<Enhet>> alleAktiveEnheterCache = Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(1)
            .build();

    private final Cache<String, Enhet> hentEnhetCache = Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();


    private final Cache<String, Enhet> hentEnhetFraOmradeCache = Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();

    public CachedNorg2Client(Norg2Client norg2Client) {
        this.norg2Client = norg2Client;
    }

    @Override
    public List<Enhet> alleAktiveEnheter() {
        return tryCacheFirst(alleAktiveEnheterCache, "ALLE_ENHETER", norg2Client::alleAktiveEnheter);
    }

    @Override
    public Enhet hentEnhet(String enhetId) {
        return tryCacheFirst(hentEnhetCache, enhetId, () -> norg2Client.hentEnhet(enhetId));
    }

    @Override
    public Enhet hentTilhorendeEnhet(String geografiskOmrade, Diskresjonskode diskresjonskode, boolean skjermet) {
        return tryCacheFirst(
                hentEnhetFraOmradeCache,
                geografiskOmrade + ofNullable(diskresjonskode).map(Diskresjonskode::name).orElse("") + skjermet,
                () -> norg2Client.hentTilhorendeEnhet(geografiskOmrade, diskresjonskode, skjermet)
        );
    }

    @Override
    public HealthCheckResult checkHealth() {
        return norg2Client.checkHealth();
    }
}
