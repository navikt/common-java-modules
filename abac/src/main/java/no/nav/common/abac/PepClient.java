package no.nav.common.abac;

import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.ResourceType;
import no.nav.common.abac.domain.request.Action;
import no.nav.common.abac.domain.response.BiasedDecisionResponse;
import no.nav.common.abac.exception.PepException;
import no.nav.common.types.feil.IngenTilgang;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static no.nav.common.abac.domain.request.Action.ActionId.READ;
import static no.nav.common.abac.domain.request.Action.ActionId.WRITE;
import static no.nav.common.abac.domain.response.Decision.Permit;

public class PepClient {

    private final Pep pep;
    private final String applicationDomain;
    private final ResourceType resourceType;

    public PepClient(Pep pep, String applicationDomain, ResourceType resourceType) {
        this.pep = pep;
        this.applicationDomain = applicationDomain;
        this.resourceType = resourceType;
    }

    public String sjekkLesetilgangTilFnr(String fnr) {
        return sjekkTilgangTilPerson(AbacPersonId.fnr(fnr), READ).getId();
    }

    public String sjekkSkrivetilgangTilFnr(String fnr) {
        return sjekkTilgangTilPerson(AbacPersonId.fnr(fnr), WRITE).getId();
    }

    public String sjekkLesetilgangTilAktorId(String aktorId) {
        return sjekkTilgangTilPerson(AbacPersonId.aktorId(aktorId), READ).getId();
    }

    public String sjekkSkrivetilgangTilAktorId(String aktorId) {
        return sjekkTilgangTilPerson(AbacPersonId.aktorId(aktorId), WRITE).getId();
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

    public AbacPersonId sjekkTilgangTilPerson(AbacPersonId personId, Action.ActionId action) throws PepException {
        return sjekkTilgangTilPerson(personId, action, resourceType);
    }

    public AbacPersonId sjekkTilgangTilPerson(AbacPersonId personId, Action.ActionId action, ResourceType resourceType) {
        return sjekkTilgangTilPerson(personId, applicationDomain, action, resourceType);
    }

    public AbacPersonId sjekkTilgangTilPerson(AbacPersonId personId, String applicationDomain, Action.ActionId action, ResourceType resourceType) {
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
