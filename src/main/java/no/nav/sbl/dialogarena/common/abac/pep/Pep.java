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

    private enum Bias {
        Permit, Deny
    }

    private final PdpService pdpService;

    public Pep(PdpService pdpService) {
        this.pdpService = pdpService;
    }

    private Request buildRequest(String oidcToken, String subjectId, String domain, String fnr, String credentialResource) {
        if (domain == null || fnr == null || credentialResource == null || (oidcToken == null && subjectId == null)) { return null; }

        final Environment environment = new Environment();
        if (oidcToken != null) {
            environment.getAttribute().add(new Attribute("no.nav.abac.attributter.environment.felles.oidc_token_body", oidcToken));
        }
        environment.getAttribute().add(new Attribute("no.nav.abac.attributter.environment.felles.pep_id", credentialResource));

        final Action action = new Action();
        action.getAttribute().add(new Attribute("urn:oasis:names:tc:xacml:1.0:action:action-id", "read"));

        final Resource resource = new Resource();
        resource.getAttribute().add(new Attribute("no.nav.abac.attributter.resource.felles.resource_type", "no.nav.abac.attributter.resource.felles.person"));
        resource.getAttribute().add(new Attribute("no.nav.abac.attributter.resource.felles.domene", domain));
        resource.getAttribute().add(new Attribute("no.nav.abac.attributter.resource.felles.person.fnr", fnr));

        return new Request()
                .withEnvironment(environment)
                .withAction(action)
                .withResource(resource);
    }

    XacmlRequest createXacmlRequest(String oidcToken, String subjectId, String domain, String fnr, String credentialResource) {
        Request request = buildRequest(oidcToken, subjectId, domain, fnr, credentialResource);
        if (request != null) {
            return new XacmlRequest().withRequest(request);
        }
        return null;
    }

    BiasedDecisionResponse evaluateWithBias(XacmlRequest request) {
        log.debug("evaluating request with bias:" + bias);
        XacmlResponse response = pdpService.askForPermission(request);

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
