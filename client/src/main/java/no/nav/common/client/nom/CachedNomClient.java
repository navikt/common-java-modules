package no.nav.common.client.nom;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.NavIdent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;

public class CachedNomClient implements NomClient {

    private final NomClient nomClient;

    private final Cache<NavIdent, VeilederNavn> veilederNavnCache;

    public CachedNomClient(NomClient nomClient, Cache<NavIdent, VeilederNavn> veilederNavnCache) {
        this.nomClient = nomClient;
        this.veilederNavnCache = veilederNavnCache;
    }

    public CachedNomClient(NomClient nomClient) {
        this.nomClient = nomClient;
        this.veilederNavnCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .maximumSize(10_000)
                .build();
    }

    @Override
    public VeilederNavn finnNavn(NavIdent navIdent) {
        return tryCacheFirst(veilederNavnCache, navIdent, () -> nomClient.finnNavn(navIdent));
    }

    @Override
    public List<VeilederNavn> finnNavn(List<NavIdent> navIdenter) {
        List<VeilederNavn> prevCachedNavn = new ArrayList<>();
        List<NavIdent> uncachedNavIdenter = new ArrayList<>();

        navIdenter.forEach(navIdent -> {
            VeilederNavn veilederNavn = veilederNavnCache.getIfPresent(navIdent);
            if (veilederNavn != null) {
                prevCachedNavn.add(veilederNavn);
            } else {
                uncachedNavIdenter.add(navIdent);
            }
        });

        List<VeilederNavn> veilederNavnListe = nomClient.finnNavn(uncachedNavIdenter);

        // Update cache
        veilederNavnListe.forEach(navn -> veilederNavnCache.put(navn.navIdent, navn));

        // Add from previously cached results
        veilederNavnListe.addAll(prevCachedNavn);

        return veilederNavnListe;
    }

    @Override
    public HealthCheckResult checkHealth() {
        return nomClient.checkHealth();
    }

}
