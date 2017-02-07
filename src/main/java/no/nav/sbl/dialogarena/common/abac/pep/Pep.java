package no.nav.sbl.dialogarena.common.abac.pep;


import no.nav.sbl.dialogarena.common.abac.PdpService;
import no.nav.sbl.dialogarena.common.abac.pep.xacml.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.xacml.Decision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Pep {

    private static final Logger log = LoggerFactory.getLogger(Pep.class);
    private static final Bias bias = Bias.Deny;
    private static final boolean failOnIndeterminateDecision = true;

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
        Environment environment = new Environment();
        if (client.getOidcToken() != null) {
            environment.getAttribute().add(new Attribute(StaticRequestValues.OIDCTOKEN_ID, client.getOidcToken()));
        }
        environment.getAttribute().add(new Attribute(StaticRequestValues.CREDENTIALRESOURCE_ID, client.getCredentialResource()));
        return environment;
    }

    AccessSubject makeAccessSubject() {
        AccessSubject accessSubject = new AccessSubject();
        accessSubject.getAttribute().add(new Attribute(StaticRequestValues.SUBJECTID_ID, client.getSubjectId()));
        accessSubject.getAttribute().add(new Attribute(StaticRequestValues.SUBJECTTYPE_ID, StaticRequestValues.SUBJECTTYPE_VALUE));
        return accessSubject;
    }

    Action makeAction() {
        Action action = new Action();
        action.getAttribute().add(new Attribute(StaticRequestValues.ACTIONID_ID, StaticRequestValues.ACTIONID_VALUE));
        return action;
    }

    Resource makeResource() {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(StaticRequestValues.RESOURCETYPE_ID, StaticRequestValues.RESOURCETYPE_VALUE));
        resource.getAttribute().add(new Attribute(StaticRequestValues.DOMAIN_ID, client.getDomain()));
        resource.getAttribute().add(new Attribute(StaticRequestValues.FNR_ID, client.getFnr()));
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
