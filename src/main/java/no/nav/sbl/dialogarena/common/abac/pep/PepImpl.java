package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.abac.xacml.NavAttributter;
import no.nav.abac.xacml.StandardAttributter;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Attribute;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Resources;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.*;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.*;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import no.nav.sbl.dialogarena.common.abac.pep.service.LdapService;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

@Component
public class PepImpl implements Pep {

    private final static int NUMBER_OF_RESPONSES_ALLOWED = 1;
    private final static Bias bias = Bias.Deny;
    private final static boolean failOnIndeterminateDecision = true;

    private enum Bias {
        Permit, Deny
    }

    private final LdapService ldapService;
    private final AbacService abacService;
    private final Client client;
    private final AuditLogger auditLogger;

    public PepImpl(LdapService ldapService, AbacService abacService) {
        this.ldapService = ldapService;
        this.abacService = abacService;
        this.client = new Client();
        auditLogger = new AuditLogger();
    }

    @Override
    public BiasedDecisionResponse isServiceCallAllowedWithOidcToken(String oidcTokenBody, String domain, String fnr) throws PepException {
        if (oidcTokenBody.contains(".")) {
            throw new IllegalArgumentException("Token contains header and/or signature. Argument should be token body.");
        }
        return isServiceCallAllowed(oidcTokenBody, null, domain, fnr, ResourceType.Person);
    }

    @Override
    public BiasedDecisionResponse isServiceCallAllowedWithIdent(String ident, String domain, String fnr) throws PepException {
        return isServiceCallAllowed(null, ident, domain, fnr, ResourceType.Person);
    }

    @Override
    public BiasedDecisionResponse isSubjectAuthorizedToSeeKode7(String subjectId, String domain) throws PepException {
        return isServiceCallAllowed(null, subjectId, domain, null, ResourceType.Kode7);
    }

    @Override
    public BiasedDecisionResponse isSubjectAuthorizedToSeeKode6(String subjectId, String domain) throws PepException {
        return isServiceCallAllowed(null, subjectId, domain, null, ResourceType.Kode6);
    }

    @Override
    public BiasedDecisionResponse isSubjectAuthorizedToSeeEgenAnsatt(String subjectId, String domain) throws PepException {
        return isServiceCallAllowed(null, subjectId, domain, null, ResourceType.EgenAnsatt);
    }

    @Override
    public boolean isSubjectMemberOfModigOppfolging(String subjectId) throws NamingException{
        return ldapService.isSubjectMemberOfModigOppfolging(subjectId);
    }

    private XacmlResponse askForPermission(XacmlRequest request) throws PepException {

        try {
            return abacService.askForPermission(request);
        } catch (AbacException e) {
            try {
                return ldapService.askForPermission(request);
            } catch (NamingException e1) {
                throw new PepException("Fallback: Verifying role in AD failed: ", e1);
            }
        } catch (UnsupportedEncodingException e) {
            throw new PepException("Cannot parse object to json request. ", e);
        } catch (IOException | NoSuchFieldException e) {
            throw new PepException(e);
        }
    }

    Environment makeEnvironment() {
        Environment environment = new Environment();
        final String oidcToken = client.getOidcToken();
        if (isNotEmpty(oidcToken)) {
            environment.getAttribute().add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, oidcToken));
        }
        environment.getAttribute().add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, client.getCredentialResource()));
        return environment;
    }

    AccessSubject makeAccessSubject() {
        AccessSubject accessSubject = new AccessSubject();
        accessSubject.getAttribute().add(new Attribute(StandardAttributter.SUBJECT_ID, client.getSubjectId()));
        accessSubject.getAttribute().add(new Attribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, "InternBruker"));
        return accessSubject;
    }

    Action makeAction() {
        Action action = new Action();
        action.getAttribute().add(new Attribute(StandardAttributter.ACTION_ID, "read"));
        return action;
    }

    Resource makeResource(ResourceType resourceType) {
        switch(resourceType) {
            case EgenAnsatt: return Resources.makeEgenAnsattResource(client);
            case Kode6: return Resources.makeKode6Resource(client);
            case Kode7: return Resources.makeKode7Resource(client);
            case Person: return Resources.makePersonResource(client);
            default: return null;
        }
    }

    Request makeRequest(ResourceType resourceType) throws PepException {
        if (Utils.invalidClientValues(client)) {
            throw new PepException("Received client values: oidc-token: " + client.getOidcToken() +
                    " subject-id: " + client.getSubjectId() + " domain: " + client.getDomain() + " fnr: " + client.getFnr() +
                    " credential resource: " + client.getCredentialResource() + "\nProvide OIDC-token or subject-ID, domain, fnr and " +
                    " name of credential resource.");
        }

        Request request = new Request()
                .withEnvironment(makeEnvironment())
                .withAction(makeAction())
                .withResource(makeResource(resourceType));
        if (client.getSubjectId() != null) {
            request.withAccessSubject(makeAccessSubject());
        }

        return request;
    }

    private XacmlRequest makeXacmlRequest(ResourceType resourceType) throws PepException {
        return new XacmlRequest().withRequest(makeRequest(resourceType));
    }

    Pep withClientValues(String oidcToken, String subjectId, String domain, String fnr, String credentialResource) {
        client
                .withOidcToken(oidcToken)
                .withSubjectId(subjectId)
                .withDomain(domain)
                .withFnr(fnr)
                .withCredentialResource(credentialResource);
        return this;
    }

    private BiasedDecisionResponse isServiceCallAllowed(String oidcToken, String subjectId, String domain, String fnr, ResourceType resourceType) throws PepException {
        auditLogger.logRequestInfo(fnr);

        String credentialResource;
        try {
            credentialResource = Utils.getApplicationProperty(CredentialConstants.SYSTEMUSER_USERNAME);
        } catch (NoSuchFieldException e) {
            throw new PepException(e);
        }
        withClientValues(oidcToken, subjectId, domain, fnr, credentialResource);

        final XacmlRequest xacmlRequest = makeXacmlRequest(resourceType);
        XacmlResponse response = askForPermission(xacmlRequest);

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

        auditLogger.logResponseInfo(originalDecision.name(), biasedDecision.name(), response.getResponse().get(0).getAssociatedAdvice(), response.isFallbackUsed());

        return new BiasedDecisionResponse(biasedDecision, response);
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
