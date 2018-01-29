package no.nav.apiapp.security;

import lombok.SneakyThrows;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.RequestData;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId.READ;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId.WRITE;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision.Permit;


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
        return sjekkTilgang(fnr, READ);
    }

    @SneakyThrows
    public String sjekkSkriveTilgangTilFnr(String fnr) {
        return sjekkTilgang(fnr, WRITE);
    }

    public void sjekkTilgangTilEnhet(String enhet) throws IngenTilgang, PepException {
        if (!harTilgangTilEnhet(enhet)) {
            throw new IngenTilgang(format("Veileder har ikke tilgang til enhet '%s'", enhet));
        }
    }

    public boolean harTilgangTilEnhet(String enhet) throws PepException {
        BiasedDecisionResponse r = pep.harTilgang(pep.nyRequest()
                .withResourceType(ResourceType.Enhet)
                .withDomain(applicationDomain)
                .withEnhet(enhet));
        return erPermit(r);
    }

    private String sjekkTilgang(String fnr, Action.ActionId action) throws PepException {
        if (erPermit(pep.harInnloggetBrukerTilgangTilPerson(fnr, applicationDomain, action, resourceType))) {
            return fnr;
        } else {
            throw new IngenTilgang();
        }
    }

    private boolean erPermit(BiasedDecisionResponse biasedDecisionResponse) {
        return ofNullable(biasedDecisionResponse)
                .map(BiasedDecisionResponse::getBiasedDecision)
                .map(d -> d == Permit)
                .orElse(false);
    }

}
