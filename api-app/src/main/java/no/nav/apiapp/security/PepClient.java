package no.nav.apiapp.security;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.common.auth.SubjectHandler;
import no.nav.sbl.dialogarena.common.abac.pep.CEFEventContext;
import no.nav.sbl.dialogarena.common.abac.pep.CEFEventResource;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.AbacPersonId;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

import javax.servlet.http.HttpServletRequest;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static no.nav.log.LogFilter.CONSUMER_ID_HEADER_NAME;
import static no.nav.log.LogFilter.resolveCallId;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId.READ;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId.WRITE;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision.Permit;


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
        return sjekkLesetilgangTilFnr(fnr, null);
    }

    public String sjekkLesetilgangTilFnr(String fnr, HttpServletRequest request) {
        AbacPersonId abacPersonId = AbacPersonId.fnr(fnr);

        CEFEventContext cefEventContext = null;
        if (request != null) {
            cefEventContext = CEFEventContext.builder()
                    .callId(resolveCallId(request))
                    .consumerId(request.getHeader(CONSUMER_ID_HEADER_NAME))
                    .requestMethod(request.getMethod())
                    .requestPath(request.getRequestURI())
                    .subjectId(SubjectHandler.getIdent().orElse(null))
                    .resource(CEFEventResource.personId(abacPersonId))
                    .build();
        }

        return sjekkTilgangTilPerson(abacPersonId, READ, cefEventContext).getId();
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
                .withEnhet(ofNullable(enhet).orElse("")), null
        );
        return erPermit(r);
    }

    public AbacPersonId sjekkTilgangTilPerson(AbacPersonId personId, Action.ActionId action) throws PepException {
        return sjekkTilgangTilPerson(personId, action, resourceType, null);
    }

    public AbacPersonId sjekkTilgangTilPerson(AbacPersonId personId, Action.ActionId action, CEFEventContext cefEventContext) throws PepException {
        return sjekkTilgangTilPerson(personId, action, resourceType, cefEventContext);
    }

    public AbacPersonId sjekkTilgangTilPerson(AbacPersonId personId, Action.ActionId action, ResourceType resourceType, CEFEventContext cefEventContext) {
        return sjekkTilgangTilPerson(personId, applicationDomain, action, resourceType, cefEventContext);
    }

    public AbacPersonId sjekkTilgangTilPerson(AbacPersonId personId, String applicationDomain, Action.ActionId action, ResourceType resourceType, CEFEventContext cefEventContext) {
        if (erPermit(pep.harInnloggetBrukerTilgangTilPerson(personId, applicationDomain, action, resourceType, cefEventContext))) {
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
