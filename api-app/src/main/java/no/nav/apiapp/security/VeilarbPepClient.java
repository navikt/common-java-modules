package no.nav.apiapp.security;

import lombok.SneakyThrows;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.AbacPersonId;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType.VeilArbPerson;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId.READ;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId.WRITE;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision.Permit;


public class VeilarbPepClient {

    private final Pep pep;
    private final String applicationDomain;


    public VeilarbPepClient(Pep pep) {
        this.pep = pep;
        this.applicationDomain = "veilarb";
    }

    @SneakyThrows
    public void sjekkLesetilgang(AbacPersonId personId) {
        sjekkTilgang(personId, READ, VeilArbPerson);
    }

    @SneakyThrows
    public void sjekkSkrivetilgang(AbacPersonId personId) {
        sjekkTilgang(personId, WRITE, VeilArbPerson);
    }

    public void sjekkTilgangTilEnhet(String enhet) {
        if (!harTilgangTilEnhet(enhet)) {
            throw new IngenTilgang(format("Ingen tilgang til enhet '%s'", enhet));
        }
    }

    public boolean harTilgangTilEnhet(String enhet) {
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

    public AbacPersonId sjekkTilgang(AbacPersonId personId, Action.ActionId action, ResourceType resourceType) {
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
