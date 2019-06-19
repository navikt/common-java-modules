package no.nav.sbl.dialogarena.common.abac.pep;

// import no.nav.abac.xacml.NavAttributter;
// import no.nav.abac.xacml.StandardAttributter;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Attribute;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Resources;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.*;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.sbl.util.EnvironmentUtils;
import no.nav.sbl.util.StringUtils;

/*
import static no.nav.abac.xacml.NavAttributter.ENVIRONMENT_FELLES_PEP_ID;
import static no.nav.abac.xacml.NavAttributter.RESOURCE_FELLES_DOMENE;
import static no.nav.abac.xacml.StandardAttributter.ACTION_ID;
*/
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

class XacmlRequestGenerator {

    Environment makeEnvironment(RequestData requestData) {
        Environment environment = new Environment();
        final String oidcToken = requestData.getOidcToken();
        if (isNotEmpty(oidcToken)) {
            environment.getAttribute().add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, oidcToken));
        }

        String samlToken = requestData.getSamlToken();
        if (isNotEmpty(samlToken)) {
            environment.getAttribute().add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_SAML_TOKEN, samlToken));
        }

        environment.getAttribute().add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, requestData.getCredentialResource()));
        return environment;
    }

    AccessSubject makeAccessSubject(RequestData requestData) {
        AccessSubject accessSubject = new AccessSubject();
        accessSubject.getAttribute().add(new Attribute(StandardAttributter.SUBJECT_ID, requestData.getSubjectId()));
        accessSubject.getAttribute().add(new Attribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, "InternBruker"));
        return accessSubject;
    }

    Action makeAction(RequestData requestData) {
        Action action = new Action();
        action.getAttribute().add(new Attribute(StandardAttributter.ACTION_ID, requestData.getAction().getId()));
        return action;
    }

    Resource makeResource(RequestData requestData) {
        switch (requestData.getResourceType()) {
            case EgenAnsatt:
                return Resources.makeEgenAnsattResource(requestData);
            case Kode6:
                return Resources.makeKode6Resource(requestData);
            case Kode7:
                return Resources.makeKode7Resource(requestData);
            case Person:
                return Resources.makePersonResource(requestData);
            case VeilArb:
                return Resources.makeVeilArbResource(requestData);
            case VeilArbPerson:
                return Resources.makeVeilArbPersonResource(requestData);
            case VeilArbUnderOppfolging:
                return Resources.makeVeilArbUnderOppfolgingResource(requestData);
            case Modia:
                return Resources.makeModiaResource(requestData);
            case Enhet:
                return Resources.makeEnhetResource(requestData);
            default:
                return null;
        }
    }

    public Request makeRequest(RequestData requestData) throws PepException {
        validateInputArguments(requestData);
        Request request = new Request()
                .withEnvironment(makeEnvironment(requestData))
                .withAction(makeAction(requestData))
                .withResource(makeResource(requestData));
        if (requestData.getSubjectId() != null) {
            request.withAccessSubject(makeAccessSubject(requestData));
        }

        return request;
    }

    private void validateInputArguments(RequestData requestData) throws PepException {
        if (Utils.invalidClientValues(requestData)) {
            throw new PepException("Received client values: " +
                    " oidc-token: " + maskToken(requestData.getOidcToken()) +
                    " saml-token: " + maskToken(requestData.getSamlToken()) +
                    " subject-id: " + requestData.getSubjectId() +
                    " domain: " + requestData.getDomain() +
                    " fnr: " + requestData.getFnr() +
                    " credential resource: " + requestData.getCredentialResource() +
                    "\nProvide OIDC-token or subject-ID, domain, fnr and " +
                    " name of credential resource.");
        }
    }

    String maskToken(String token) {
        return StringUtils.of(token)
                .map(s -> StringUtils.substring(s, 0,8) + "*********")
                .orElse("");
    }

    public static XacmlRequest getPingRequest() {
        Action action = new Action();
        action.addAttribute(new Attribute(StandardAttributter.ACTION_ID, "ping"));

        Resource resource = new Resource();
        resource.addAttribute(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, "veilarb"));

        Environment environment = new Environment();
        environment.addAttribute(new Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, EnvironmentUtils.requireApplicationName()));
        return new XacmlRequest().withRequest(new Request()
                .withAction(action)
                .withResource(resource)
                .withEnvironment(environment)
        );

    }
}
