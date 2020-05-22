package no.nav.apiapp.security;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.common.auth.SubjectHandler;
import no.nav.sbl.dialogarena.common.abac.pep.*;
import no.nav.sbl.dialogarena.common.abac.pep.cef.CefEventContext;
import no.nav.sbl.dialogarena.common.abac.pep.cef.CefEventResource;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static no.nav.log.LogFilter.resolveCallId;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId.READ;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId.WRITE;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision.Permit;
import static no.nav.sbl.util.EnvironmentUtils.getApplicationName;
import static no.nav.sbl.util.EnvironmentUtils.requireApplicationName;


public class PepClient {

    private static final Logger log = LoggerFactory.getLogger(PepClient.class);

    private final Pep pep;
    private final String applicationDomain;
    private final ResourceType resourceType;

    public PepClient(Pep pep, String applicationDomain, ResourceType resourceType) {
        this.pep = pep;
        this.applicationDomain = applicationDomain;
        this.resourceType = resourceType;
    }

    public String sjekkLesetilgangTilFnr(String fnr) {
        AbacPersonId abacPersonId = AbacPersonId.fnr(fnr);
        CefEventContext cefEventContext = lagCefEventContext(CefEventResource.personId(abacPersonId)).orElse(null);
        return sjekkTilgangTilPerson(abacPersonId, READ, cefEventContext).getId();
    }

    public String sjekkSkrivetilgangTilFnr(String fnr) {
        AbacPersonId abacPersonId = AbacPersonId.fnr(fnr);
        CefEventContext cefEventContext = lagCefEventContext(CefEventResource.personId(abacPersonId)).orElse(null);
        return sjekkTilgangTilPerson(abacPersonId, WRITE, cefEventContext).getId();
    }

    public String sjekkLesetilgangTilAktorId(String aktorId) {
        AbacPersonId abacPersonId = AbacPersonId.aktorId(aktorId);
        CefEventContext cefEventContext = lagCefEventContext(CefEventResource.personId(abacPersonId)).orElse(null);
        return sjekkTilgangTilPerson(abacPersonId, READ, cefEventContext).getId();
    }

    public String sjekkSkrivetilgangTilAktorId(String aktorId) {
        AbacPersonId abacPersonId = AbacPersonId.aktorId(aktorId);
        CefEventContext cefEventContext = lagCefEventContext(CefEventResource.personId(abacPersonId)).orElse(null);
        return sjekkTilgangTilPerson(abacPersonId, WRITE).getId();
    }

    public void sjekkTilgangTilEnhet(String enhet) throws IngenTilgang, PepException {
        if (!harTilgangTilEnhet(enhet)) {
            throw new IngenTilgang(format("Ingen tilgang til enhet '%s'", enhet));
        }
    }

    public boolean harTilgangTilEnhet(String enhet) throws PepException {
        RequestData requestData = pep.nyRequest()
                .withResourceType(ResourceType.Enhet)
                .withDomain(applicationDomain)
                // ABAC feiler hvis man spør om tilgang til udefinerte enheter (null) men tillater å spørre om tilgang
                // til enheter som ikke finnes (f.eks. tom streng)
                // Ved å konvertere null til tom streng muliggjør vi å spørre om tilgang til enhet for brukere som
                // ikke har enhet. Sluttbrukere da få permit mens veiledere vil få deny.
                .withEnhet(ofNullable(enhet).orElse(""));

        CefEventContext cefEventContext = lagCefEventContext(CefEventResource.enhetId(enhet)).orElse(null);

        BiasedDecisionResponse r = pep.harTilgang(requestData, cefEventContext);
        return erPermit(r);
    }

    public AbacPersonId sjekkTilgangTilPerson(AbacPersonId personId, Action.ActionId action) throws PepException {
        return sjekkTilgangTilPerson(personId, action, resourceType, null);
    }

    public AbacPersonId sjekkTilgangTilPerson(AbacPersonId personId, Action.ActionId action, CefEventContext cefEventContext) throws PepException {
        return sjekkTilgangTilPerson(personId, action, resourceType, cefEventContext);
    }

    public AbacPersonId sjekkTilgangTilPerson(AbacPersonId personId, Action.ActionId action, ResourceType resourceType, CefEventContext cefEventContext) {
        return sjekkTilgangTilPerson(personId, applicationDomain, action, resourceType, cefEventContext);
    }

    public AbacPersonId sjekkTilgangTilPerson(AbacPersonId personId, String applicationDomain, Action.ActionId action, ResourceType resourceType, CefEventContext cefEventContext) {
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

    private Optional<CefEventContext> lagCefEventContext(CefEventResource resource) {
        Optional<HttpServletRequest> httpServletRequest =
                Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                        .filter(x -> x instanceof ServletRequestAttributes)
                        .map(x -> (ServletRequestAttributes) x)
                        .map(ServletRequestAttributes::getRequest);


        try {
            return httpServletRequest.flatMap(request ->
                    getApplicationName().map(appName ->
                            CefEventContext.builder()
                                    .applicationName(requireApplicationName())
                                    .callId(resolveCallId(request))
                                    .requestMethod(request.getMethod())
                                    .requestPath(request.getRequestURI())
                                    .subjectId(SubjectHandler.getIdent().orElse(null))
                                    .resource(resource)
                                    .build()));
        } catch (Exception e) {
            log.warn("Kunne ikke lage CefEventContext:", e);
            return Optional.empty();
        }
    }
}
