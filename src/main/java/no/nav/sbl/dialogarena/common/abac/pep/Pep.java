package no.nav.sbl.dialogarena.common.abac.pep;


import no.nav.sbl.dialogarena.common.abac.PdpService;
import no.nav.sbl.dialogarena.common.abac.pep.xacml.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.xacml.Decision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Pep {

    private final static Logger log = LoggerFactory.getLogger(Pep.class);
    private final Bias bias = Bias.Deny;
    private final boolean failOnIndeterminateDecision = true;

    private static final String OIDCTOKEN_ID = "no.nav.abac.attributter.environment.felles.oidc_token_body";
    private static final String CREDENTIALRESOURCE_ID = "no.nav.abac.attributter.environment.felles.pep_id";
    private static final String SUBJECTID_ID = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";
    private static final String SUBJECTTYPE_ID = "no.nav.abac.attributter.subject.felles.subjectType";
    private static final String SUBJECTTYPE_VALUE = "InternBruker";
    private static final String ACTIONID_ID = "urn:oasis:names:tc:xacml:1.0:action:action-id";
    private static final String ACTIONID_VALUE = "read";
    private static final String RESOURCETYPE_ID = "no.nav.abac.attributter.resource.felles.resource_type";
    private static final String RESOURCETYPE_VALUE = "no.nav.abac.attributter.resource.felles.person";
    private static final String DOMAIN_ID = "no.nav.abac.attributter.resource.felles.domene";
    private static final String FNR_ID = "no.nav.abac.attributter.resource.felles.person.fnr";

    private enum Bias {
        Permit, Deny
    }

    private final PdpService pdpService;
    private Client client;

    public Pep(PdpService pdpService) {
        this.pdpService = pdpService;
        this.client = new Client();
    }

    Environment makeEnvironment() {
        final Environment environment = new Environment();
        if (client.getOidcToken() != null) {
            environment.getAttribute().add(new Attribute(OIDCTOKEN_ID, client.getOidcToken()));
        }
        environment.getAttribute().add(new Attribute(CREDENTIALRESOURCE_ID, client.getCredentialResource()));
        return environment;
    }

    AccessSubject makeAccessSubject() {
        final AccessSubject accessSubject = new AccessSubject();
        accessSubject.getAttribute().add(new Attribute(SUBJECTID_ID, client.getSubjectId()));
        accessSubject.getAttribute().add(new Attribute(SUBJECTTYPE_ID, SUBJECTTYPE_VALUE));
        return accessSubject;
    }

    Action makeAction() {
        final Action action = new Action();
        action.getAttribute().add(new Attribute(ACTIONID_ID, ACTIONID_VALUE));
        return action;
    }

    Resource makeResource() {
        final Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(RESOURCETYPE_ID, RESOURCETYPE_VALUE));
        resource.getAttribute().add(new Attribute(DOMAIN_ID, client.getDomain()));
        resource.getAttribute().add(new Attribute(FNR_ID, client.getFnr()));
        return resource;
    }

    Request makeRequest() {
        if (client.getDomain() == null || client.getFnr() == null || client.getCredentialResource() == null ||
                (client.getOidcToken() == null && client.getSubjectId() == null)) { return null; }

        Request request = new Request()
                .withEnvironment(makeEnvironment())
                .withAction(makeAction())
                .withResource(makeResource());
        if (client.getSubjectId() != null) {
            request.withAccessSubject(makeAccessSubject());
        }
        return request;
    }

    XacmlRequest makeXacmlRequest() {
        Request request = makeRequest();
        if (request != null) {
            return new XacmlRequest().withRequest(request);
        }
        throw new PepException("Did not receive sufficient values. Provide OIDC-token or subject-ID, domain, fnr and" +
                "name of credential resource");
    }

    Client setClientValues(String oidcToken, String subjectId, String domain, String fnr, String credentialResource) {
        client
                .withOidcToken(oidcToken)
                .withSubjectId(subjectId)
                .withDomain(domain)
                .withFnr(fnr)
                .withCredentialResource(credentialResource);
        return client;
    }

    BiasedDecisionResponse evaluateWithBias(String oidcToken, String subjectId, String domain, String fnr, String credentialResource) {
        setClientValues(oidcToken, subjectId, domain, fnr, credentialResource);

        log.debug("evaluating request with bias:" + bias);
        XacmlResponse response = pdpService.askForPermission(makeXacmlRequest());

        Decision originalDecision = response.getDecision();
        Decision biasedDecision = createBiasedDecision(originalDecision);

        if (failOnIndeterminateDecision && originalDecision == Decision.Indeterminate) {
            throw new PepException("received decision " + originalDecision + " from PDP. This should never happen. "
                    + "Fix policy and/or PEP to send proper attributes.");
        }
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
