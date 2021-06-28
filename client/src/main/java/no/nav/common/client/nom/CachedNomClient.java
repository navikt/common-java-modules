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

    private final Cache<NavIdent, VeilederVisningsnavn> veilederVisningsnavnCache;

    public CachedNomClient(NomClient nomClient, Cache<NavIdent, VeilederVisningsnavn> veilederVisningsnavnCache) {
        this.nomClient = nomClient;
        this.veilederVisningsnavnCache = veilederVisningsnavnCache;
    }



    public CachedNomClient(NomClient nomClient) {
        this.nomClient = nomClient;
        this.veilederVisningsnavnCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .maximumSize(10_000)
                .build();
    }



    @Override
    public VeilederVisningsnavn finnVisningsnavn(NavIdent navIdent) {
        return tryCacheFirst(veilederVisningsnavnCache, navIdent, () -> nomClient.finnVisningsnavn(navIdent));
    }

    @Override
    public List<VeilederVisningsnavn> finnVisningsnavn(List<NavIdent> navIdenter) {
        List<VeilederVisningsnavn> veilederVisningsnavnListe = new ArrayList<>();
        List<NavIdent> uncachedNavIdenter = new ArrayList<>();

        navIdenter.forEach(navIdent -> {
            VeilederVisningsnavn veilederVisningsnavn = veilederVisningsnavnCache.getIfPresent(navIdent);
            if (veilederVisningsnavn != null) {
                veilederVisningsnavnListe.add(veilederVisningsnavn);
            } else {
                uncachedNavIdenter.add(navIdent);
            }
        });

        if (!uncachedNavIdenter.isEmpty()) {
            List<VeilederVisningsnavn> funnetVisningsNavn = nomClient.finnVisningsnavn(uncachedNavIdenter);

            funnetVisningsNavn.forEach(visningsnavn -> veilederVisningsnavnCache.put(visningsnavn.navIdent, visningsnavn));

            veilederVisningsnavnListe.addAll(funnetVisningsNavn);
        }

        return veilederVisningsnavnListe;
    }

    @Override
    public HealthCheckResult checkHealth() {
        return nomClient.checkHealth();
    }

}
