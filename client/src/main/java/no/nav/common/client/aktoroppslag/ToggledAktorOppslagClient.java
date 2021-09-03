package no.nav.common.client.aktoroppslag;

import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.Fnr;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Midlertidig klient for å toggle mellom bruk av Aktørregisteret og PDL som kilde for aktøroppslag.
 */
public class ToggledAktorOppslagClient implements AktorOppslagClient {

    private final AktorregisterHttpClient aktorregisterOppslagClient;

    private final PdlAktorOppslagClient pdlAktorOppslagClient;

    private final Supplier<Boolean> brukPdlAktorOppslag;

    public ToggledAktorOppslagClient(
            AktorregisterHttpClient aktorregisterOppslagClient,
            PdlAktorOppslagClient pdlAktorOppslagClient,
            Supplier<Boolean> brukPdlAktorOppslag
    ) {
        this.aktorregisterOppslagClient = aktorregisterOppslagClient;
        this.pdlAktorOppslagClient = pdlAktorOppslagClient;
        this.brukPdlAktorOppslag = brukPdlAktorOppslag;
    }

    @Override
    public Fnr hentFnr(AktorId aktorId) {
        return getClient().hentFnr(aktorId);
    }

    @Override
    public AktorId hentAktorId(Fnr fnr) {
        return getClient().hentAktorId(fnr);
    }

    @Override
    public Map<AktorId, Fnr> hentFnrBolk(List<AktorId> aktorIdListe) {
        return getClient().hentFnrBolk(aktorIdListe);
    }

    @Override
    public Map<Fnr, AktorId> hentAktorIdBolk(List<Fnr> fnrListe) {
        return getClient().hentAktorIdBolk(fnrListe);
    }

    @Override
    public BrukerIdenter hentIdenter(EksternBrukerId brukerId) {
        return getClient().hentIdenter(brukerId);
    }

    @Override
    public HealthCheckResult checkHealth() {
        return getClient().checkHealth();
    }

    private AktorOppslagClient getClient() {
        return brukPdlAktorOppslag.get() ? pdlAktorOppslagClient : aktorregisterOppslagClient;
    }

}
