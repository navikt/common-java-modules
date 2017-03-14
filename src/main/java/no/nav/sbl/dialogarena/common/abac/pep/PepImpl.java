package no.nav.sbl.dialogarena.common.abac.pep;


import no.nav.abac.xacml.NavAttributter;
import no.nav.abac.xacml.StandardAttributter;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Attribute;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.*;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.*;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import no.nav.sbl.dialogarena.common.abac.pep.service.LdapService;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
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
    public BiasedDecisionResponse isServiceCallAllowedWithToken(String oidcToken, String domain, String fnr) throws PepException {
        return isServiceCallAllowed(oidcToken, null, domain, fnr);
    }

    @Override
    public BiasedDecisionResponse isServiceCallAllowedWithIdent(String ident, String domain, String fnr) throws PepException {
        return isServiceCallAllowed(null, ident, domain, fnr);
    }

    private XacmlResponse askForPermission(XacmlRequest request) throws PepException {

        try {
            return abacService.askForPermission(request);
        } catch (AbacException e) {
            LOG.warn("Error calling ABAC ", e);
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

    Resource makeResource() {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_FELLES_PERSON));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, client.getDomain()));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_PERSON_FNR, client.getFnr()));
        return resource;
    }

    Request makeRequest() throws PepException {
        if (Utils.invalidClientValues(client)) {
            throw new PepException("Received client values: oidc-token: " + client.getOidcToken() +
                    " subject-id: " + client.getSubjectId() + " domain: " + client.getDomain() + " fnr: " + client.getFnr() +
                    " credential resource: " + client.getCredentialResource() + "\nProvide OIDC-token or subject-ID, domain, fnr and " +
                    " name of credential resource.");
        }

        Request request = new Request()
                .withEnvironment(makeEnvironment())
                .withAction(makeAction())
                .withResource(makeResource());
        if (client.getSubjectId() != null) {
            request.withAccessSubject(makeAccessSubject());
        }

        return request;
    }

    private XacmlRequest makeXacmlRequest() throws PepException {
        return new XacmlRequest().withRequest(makeRequest());
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

    private BiasedDecisionResponse isServiceCallAllowed(String oidcToken, String subjectId, String domain, String fnr) throws PepException {
        auditLogger.logRequestInfo(fnr);

        String credentialResource;
        try {
            credentialResource = Utils.getApplicationProperty(CredentialConstants.SYSTEMUSER_USERNAME);
        } catch (NoSuchFieldException e) {
            throw new PepException(e);
        }
        withClientValues(oidcToken, subjectId, domain, fnr, credentialResource);

        final XacmlRequest xacmlRequest = makeXacmlRequest();
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
