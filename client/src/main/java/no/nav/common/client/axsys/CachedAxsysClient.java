package no.nav.common.client.axsys;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;

public class CachedAxsysClient implements AxsysClient {

    private final AxsysClient axsysClient;

    private final Cache<NavIdent, AxsysEnheter> hentTilgangerCache = Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();


    private final Cache<EnhetId, List<NavIdent> > hentAnsatteCache = Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();

    public CachedAxsysClient(AxsysClient axsysClient) {
        this.axsysClient = axsysClient;
    }

    @Override
    public List<NavIdent> hentAnsatte(EnhetId enhetId){
        return tryCacheFirst(hentAnsatteCache, enhetId, () -> axsysClient.hentAnsatte(enhetId));
    }

    @Override
    public AxsysEnheter hentTilganger(NavIdent veileder) {
        return tryCacheFirst(hentTilgangerCache, veileder, () -> axsysClient.hentTilganger(veileder));
    }

    @Override
    public HealthCheckResult checkHealth() {
        return axsysClient.checkHealth();
    }
}
