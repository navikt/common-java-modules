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

    private final Cache<EnhetId, List<NavIdent>> hentAnsatteCache;

    private final Cache<NavIdent, List<AxsysEnhet>> hentTilgangerCache;

    public CachedAxsysClient(AxsysClient axsysClient) {
        this.axsysClient = axsysClient;

        this.hentAnsatteCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .maximumSize(500)
                .build();

        this.hentTilgangerCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .maximumSize(10_000)
                .build();
    }

    public CachedAxsysClient(AxsysClient axsysClient, Cache<NavIdent, List<AxsysEnhet>> hentTilgangerCache, Cache<EnhetId, List<NavIdent>> hentAnsatteCache) {
        this.axsysClient = axsysClient;
        this.hentTilgangerCache = hentTilgangerCache;
        this.hentAnsatteCache = hentAnsatteCache;
    }

    @Override
    public List<NavIdent> hentAnsatte(EnhetId enhetId){
        return tryCacheFirst(hentAnsatteCache, enhetId, () -> axsysClient.hentAnsatte(enhetId));
    }

    @Override
    public List<AxsysEnhet> hentTilganger(NavIdent navIdent) {
        return tryCacheFirst(hentTilgangerCache, navIdent, () -> axsysClient.hentTilganger(navIdent));
    }

    @Override
    public HealthCheckResult checkHealth() {
        return axsysClient.checkHealth();
    }
}
