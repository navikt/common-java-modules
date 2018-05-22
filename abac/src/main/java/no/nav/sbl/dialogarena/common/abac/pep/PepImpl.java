package no.nav.sbl.dialogarena.common.abac.pep;

import lombok.SneakyThrows;
import no.nav.abac.xacml.NavAttributter;
import no.nav.abac.xacml.StandardAttributter;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Attribute;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.*;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static no.nav.sbl.dialogarena.common.abac.pep.utils.SecurityUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class PepImpl implements Pep {

    private final static int NUMBER_OF_RESPONSES_ALLOWED = 1;
    private final static Bias bias = Bias.Deny;
    private final static boolean failOnIndeterminateDecision = true;
    private static final Logger LOG = getLogger(PepImpl.class);

    private enum Bias {
        Permit, Deny
    }

    private final AbacService abacService;
    private final AuditLogger auditLogger;

    public PepImpl(AbacService abacService) {
        this.abacService = abacService;
        auditLogger = new AuditLogger();
    }

    @Override
    public BiasedDecisionResponse isServiceCallAllowedWithOidcToken(String oidcTokenBody, String domain, String fnr) throws PepException {
        validateFnr(fnr);
        final String token = extractOidcTokenBody(oidcTokenBody);
        return isServiceCallAllowed(token, null, domain, fnr, ResourceType.Person);
    }

    @Override
    public BiasedDecisionResponse isServiceCallAllowedWithIdent(String ident, String domain, String fnr) throws PepException {
        validateFnr(fnr);
        return isServiceCallAllowed(null, ident, domain, fnr, ResourceType.Person);
    }

    @Override
    public BiasedDecisionResponse isSubjectAuthorizedToSeeKode7(String token, String domain) throws PepException {
        final String tokenBody = extractOidcTokenBody(token);
        return isServiceCallAllowed(tokenBody, null, domain, null, ResourceType.Kode7);
    }

    @Override
    public BiasedDecisionResponse isSubjectAuthorizedToSeeKode6(String token, String domain) throws PepException {
        final String tokenBody = extractOidcTokenBody(token);
        return isServiceCallAllowed(tokenBody, null, domain, null, ResourceType.Kode6);
    }

    @Override
    public BiasedDecisionResponse isSubjectAuthorizedToSeeEgenAnsatt(String token, String domain) throws PepException {
        final String tokenBody = extractOidcTokenBody(token);
        return isServiceCallAllowed(tokenBody, null, domain, null, ResourceType.EgenAnsatt);
    }

    @Override
    public BiasedDecisionResponse isSubjectMemberOfModiaOppfolging(String token, String domain) throws PepException {
        final String tokenBody = extractOidcTokenBody(token);
        return isServiceCallAllowed(tokenBody, null, domain, null, ResourceType.VeilArb);
    }

    @Override
    public BiasedDecisionResponse harInnloggetBrukerTilgangTilPerson(String fnr, String domain, Action.ActionId action, ResourceType resourceType) throws PepException {
        validateFnr(fnr);
        return harTilgang(nyRequest()
                .withFnr(fnr)
                .withAction(action)
                .withDomain(domain)
                .withResourceType(resourceType)
        );
    }

    @Override
    public BiasedDecisionResponse harInnloggetBrukerTilgangTilPerson(String fnr, String domain) throws PepException {
        return harInnloggetBrukerTilgangTilPerson(fnr, domain, Action.ActionId.READ, ResourceType.Person);
    }

    @Override
    public void ping() throws PepException {
        Decision biasedDecision;

        try {
            XacmlResponse response = abacService.askForPermission(XacmlRequestGenerator.getEmptyRequest());
            Decision originalDecision = response.getResponse().get(0).getDecision();
            biasedDecision = createBiasedDecision(originalDecision);

        } catch (NoSuchFieldException | AbacException | IOException e) {
            throw new PepException("Feil ved kall til abac", e);
        }


        if (biasedDecision.equals(Decision.Permit)) {
            throw new PepException("Ping call should return Deny not Permit");
        }
    }

    private BiasedDecisionResponse isServiceCallAllowed(String oidcToken, String subjectId, String domain, String fnr, ResourceType resourceType) throws PepException {
        return harTilgang(buildRequest()
                .withOidcToken(oidcToken)
                .withSubjectId(subjectId)
                .withDomain(domain)
                .withFnr(fnr)
                .withResourceType(resourceType)
        );
    }

    @Override
    public RequestData nyRequest() throws PepException {
        return buildRequest()
                .withSamlToken(getSamlToken().orElse(null))
                .withOidcToken(getOidcToken().orElse(null));
    }

    @SneakyThrows
    private RequestData buildRequest() {
        return new RequestData().withCredentialResource(getCredentialResource());
    }

    @SneakyThrows
    @Override
    public BiasedDecisionResponse harTilgang(RequestData requestData) {
        return harTilgang(new XacmlRequestGenerator().makeRequest(requestData));
    }

    @SneakyThrows
    @Override
    public BiasedDecisionResponse harTilgang(Request request) throws PepException {
        auditLogger.logRequestInfo(request);

        XacmlResponse response = askForPermission(new XacmlRequest().withRequest(request));

        if (response.getResponse().size() > NUMBER_OF_RESPONSES_ALLOWED) {
            throw new PepException("Pep is giving " + response.getResponse().size() + " responses. Only "
                    + NUMBER_OF_RESPONSES_ALLOWED + " is supported.");
        }

        Decision originalDecision = response.getResponse().get(0).getDecision();
        Decision biasedDecision = createBiasedDecision(originalDecision);

        if (failOnIndeterminateDecision && originalDecision == Decision.Indeterminate) {
            throw new PepException("received decision " + originalDecision + " from PDP. This should never happen. "
                    + "Fix policy and/or PEP to send proper attributes.");
        }

        auditLogger.logResponseInfo(biasedDecision.name(), response, request);

        return new BiasedDecisionResponse(biasedDecision, response);
    }

    @Override
    public BiasedDecisionResponse harTilgangTilEnhet(String enhet, String systembruker, String domain) throws PepException {
        Request request = lagHarTilgangTilEnhetRequest(enhet, systembruker, domain);

        return harTilgang(request);
    }

    private Request lagHarTilgangTilEnhetRequest(String enhet, String systembruker, String domain) {
        Environment environment = new Environment();
        environment.addAttribute(new Attribute(NavAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, getOidcToken().orElse(null)));
        environment.addAttribute(new Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, systembruker));

        Action action = new Action();
        action.addAttribute(new Attribute(StandardAttributter.ACTION_ID, "READ"));

        AccessSubject accessSubject = new AccessSubject();

        Resource resource = new Resource();
        resource.addAttribute(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_FELLES_ENHET));
        resource.addAttribute(new Attribute(NavAttributter.RESOURCE_FELLES_ENHET, enhet));
        resource.addAttribute(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, domain));

        return new Request()
                .withEnvironment(environment)
                .withAction(action)
                .withAccessSubject(accessSubject)
                .withResource(resource);
    }

    private String getCredentialResource() throws PepException {
        try {
            return Utils.getApplicationProperty(CredentialConstants.SYSTEMUSER_USERNAME);
        } catch (Exception e) {
            throw new PepException(e);
        }
    }

    private void validateFnr(String fnr) {
        if (!StringUtils.isNumeric(fnr) || fnr.length() != 11) {
            final String message = "Fnr " + fnr + " is not valid";
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }
    }

    private XacmlResponse askForPermission(XacmlRequest request) throws PepException {
        try {
            return abacService.askForPermission(request);
        } catch (AbacException e) {
            throw new PepException(e);
        } catch (UnsupportedEncodingException e) {
            throw new PepException("Cannot parse object to json request. ", e);
        } catch (IOException | NoSuchFieldException e) {
            throw new PepException(e);
        }
    }

    private Decision createBiasedDecision(Decision originalDecision) {
        switch (originalDecision) {
            case NotApplicable:
                return Decision.valueOf(bias.name());
            case Indeterminate:
                return Decision.valueOf(bias.name());
            default:
                return originalDecision;
        }
    }

}
