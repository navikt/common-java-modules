package no.nav.apiapp.security;

import lombok.SneakyThrows;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;


public class PepClient {

    private final Pep pep;
    private final String applicationDomain;
    private final ResourceType resourceType;

    @Deprecated
    public PepClient(Pep pep, String applicationDomain) {
        this(pep, applicationDomain, ResourceType.Person);
    }

    public PepClient(Pep pep, String applicationDomain, ResourceType resourceType) {
        this.pep = pep;
        this.applicationDomain = applicationDomain;
        this.resourceType = resourceType;
    }

    @SneakyThrows
    @Deprecated
    public String sjekkTilgangTilFnr(String fnr) {
        return sjekkLeseTilgangTilFnr(fnr);
    }

    @SneakyThrows
    public String sjekkLeseTilgangTilFnr(String fnr) {
        return sjekkTilgang(fnr, Action.ActionId.READ);
    }

    @SneakyThrows
    public String sjekkSkriveTilgangTilFnr(String fnr) {
        return sjekkTilgang(fnr, Action.ActionId.WRITE);
    }

    private String sjekkTilgang(String fnr, Action.ActionId action) throws PepException {
        if (Decision.Permit == pep.harInnloggetBrukerTilgangTilPerson(fnr, applicationDomain, action, resourceType).getBiasedDecision()) {
            return fnr;
        } else {
            throw new IngenTilgang();
        }
    }

}
