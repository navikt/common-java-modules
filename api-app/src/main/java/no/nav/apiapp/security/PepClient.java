package no.nav.apiapp.security;

import lombok.SneakyThrows;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.AbacPersonId;
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
        return sjekkTilgang(AbacPersonId.fnr(fnr), READ).getId();
    }

    @SneakyThrows
    public String sjekkSkriveTilgangTilFnr(String fnr) {
        return sjekkTilgang(AbacPersonId.fnr(fnr), WRITE).getId();
    }

    @SneakyThrows
    public void sjekkLesetilgang(AbacPersonId personId) {
        sjekkTilgang(personId, READ, resourceType);
    }

    @SneakyThrows
    public void sjekkSkrivetilgang(AbacPersonId personId) {
        sjekkTilgang(personId, WRITE, resourceType);
    }

    public void sjekkTilgangTilEnhet(String enhet) throws IngenTilgang, PepException {
        if (!harTilgangTilEnhet(enhet)) {
            throw new IngenTilgang(format("Ingen tilgang til enhet '%s'", enhet));
        }
    }

    public boolean harTilgangTilEnhet(String enhet) throws PepException {
        BiasedDecisionResponse r = pep.harTilgang(pep.nyRequest()
                .withResourceType(ResourceType.Enhet)
                .withDomain(applicationDomain)
                // ABAC feiler hvis man spør om tilgang til udefinerte enheter (null) men tillater å spørre om tilgang
                // til enheter som ikke finnes (f.eks. tom streng)
                // Ved å konvertere null til tom streng muliggjør vi å spørre om tilgang til enhet for brukere som
                // ikke har enhet. Sluttbrukere da få permit mens veiledere vil få deny.
                .withEnhet(ofNullable(enhet).orElse(""))
        );
        return erPermit(r);
    }

    public AbacPersonId sjekkTilgang(AbacPersonId personId, Action.ActionId action) throws PepException {
        return sjekkTilgang(personId, action, resourceType);
    }

    public AbacPersonId sjekkTilgang(AbacPersonId personId, Action.ActionId action, ResourceType resourceType) {
        return sjekkTilgang(personId, applicationDomain, action, resourceType);
    }

    public AbacPersonId sjekkTilgang(AbacPersonId personId, String applicationDomain, Action.ActionId action, ResourceType resourceType) {
        if (erPermit(pep.harInnloggetBrukerTilgangTilPerson(personId, applicationDomain, action, resourceType))) {
            return personId;
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
