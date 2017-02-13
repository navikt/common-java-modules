package no.nav.sbl.dialogarena.common.abac.pep;


import no.nav.abac.xacml.NavAttributter;
import no.nav.abac.xacml.StandardAttributter;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Attribute;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.*;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

@Component
class Pep {

    private final static Logger log = LoggerFactory.getLogger(Pep.class);
    private final static int NUMBER_OF_RESPONSES_ALLOWED = 1;
    private final static Bias bias = Bias.Deny;
    private final static boolean failOnIndeterminateDecision = true;

    private enum Bias {
        Permit, Deny
    }

    private final PdpService pdpService;
    private Client client;
    private FileHandler fh;

    public Pep(PdpService pdpService) {
        this.pdpService = pdpService;
        this.client = new Client();

    }

    private void addFileHandlerToLogger() throws SecurityException {
        try {
            this.fh = new FileHandler("C:\\Users\\e148211\\abac\\LoggFil.log");
        } catch (Exception e) {
            e.printStackTrace();
        }
        fh.setFormatter(new SimpleFormatter());
        
    }

    Environment makeEnvironment() {
        Environment environment = new Environment();
        if (client.getOidcToken() != null) {
            environment.getAttribute().add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, client.getOidcToken()));
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

    Request makeRequest() {
        if (Utils.invalidClientValues(client)) {
            throw new PepException("Received client values: oidc-token:" + client.getOidcToken() +
            "subject-id:" + client.getSubjectId() + "domain: " + client.getDomain() + "fnr: " + client.getFnr() +
            "credential resource: " + client.getCredentialResource() + "\nProvide OIDC-token or subject-ID, domain, fnr and" +
                    "name of credential resource");
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

    XacmlRequest makeXacmlRequest() {
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

    void logRequestInfoToConsole(String navIdent, String fnr) {
        DateFormat df = new SimpleDateFormat("YYYY-MM-DD HH:MM:SS");
        Date date = new Date();
        log.info("Time of request: " + df.format(date));
        log.info("NAV-ident: " + navIdent);
        log.info("Fnr: " + fnr);
    }

    void logResponseInfoToConsole(String httpResponseCode, String abacDecision, String biasedDecision, List<Advice> advises) {
        //log.info("HTTP response code: " + httpResponseCode);
        log.info("Decision value from ABAC: " + abacDecision);
        log.info("Biased decision: " + biasedDecision);
        if (advises != null) { log.info(advises.toString()); }
    }

    BiasedDecisionResponse evaluateWithBias(String oidcToken, String subjectId, String domain, String fnr, String credentialResource) {
        if (oidcToken != null) { logRequestInfoToConsole(oidcToken, fnr); }
        else { logRequestInfoToConsole(subjectId, fnr); }

        withClientValues(oidcToken, subjectId, domain, fnr, credentialResource);

        log.debug("evaluating request with bias:" + bias);
        XacmlResponse response = pdpService.askForPermission(makeXacmlRequest());

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

        logResponseInfoToConsole("", originalDecision.name(), biasedDecision.name(), response.getResponse().get(0).getAssociatedAdvice());

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
