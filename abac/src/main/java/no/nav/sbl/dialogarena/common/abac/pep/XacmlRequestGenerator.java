package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.abac.xacml.NavAttributter;
import no.nav.abac.xacml.StandardAttributter;
import no.nav.sbl.dialogarena.common.abac.pep.domain.*;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.*;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

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
            // TODO er det innafor å legge ved oidc-token/saml-token i noe som sannsynligvis logges?
            throw new PepException("Received client values: " +
                    " oidc-token: " + requestData.getOidcToken() +
                    " saml-token: " + requestData.getSamlToken() +
                    " subject-id: " + requestData.getSubjectId() +
                    " domain: " + requestData.getDomain() +
                    " fnr: " + requestData.getFnr() +
                    " credential resource: " + requestData.getCredentialResource() +
                    "\nProvide OIDC-token or subject-ID, domain, fnr and " +
                    " name of credential resource.");
        }
    }


    public static XacmlRequest getEmptyRequest() {
        return new XacmlRequest().withRequest(new Request());

    }
}
