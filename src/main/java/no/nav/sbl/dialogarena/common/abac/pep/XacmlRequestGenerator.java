package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.abac.xacml.NavAttributter;
import no.nav.abac.xacml.StandardAttributter;
import no.nav.sbl.dialogarena.common.abac.pep.domain.*;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.*;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

class XacmlRequestGenerator {

    private final Client client;

    private final XacmlRequest xacmlRequest;

    XacmlRequestGenerator(Client client) throws PepException {
        this.client = client;
        validateInputArguments();

        xacmlRequest = new XacmlRequest()
                .withRequest(makeRequest(client.getResourceType()));
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
        switch (resourceType) {
            case EgenAnsatt:
                return Resources.makeEgenAnsattResource(client);
            case Kode6:
                return Resources.makeKode6Resource(client);
            case Kode7:
                return Resources.makeKode7Resource(client);
            case Person:
                return Resources.makePersonResource(client);
            default:
                return null;
        }
    }

    Request makeRequest(ResourceType resourceType) throws PepException {

        Request request = new Request()
                .withEnvironment(makeEnvironment())
                .withAction(makeAction())
                .withResource(makeResource(resourceType));
        if (client.getSubjectId() != null) {
            request.withAccessSubject(makeAccessSubject());
        }

        return request;
    }

    private void validateInputArguments() throws PepException {
        if (Utils.invalidClientValues(client)) {
            throw new PepException("Received client values: oidc-token: " + client.getOidcToken() +
                    " subject-id: " + client.getSubjectId() + " domain: " + client.getDomain() + " fnr: " + client.getFnr() +
                    " credential resource: " + client.getCredentialResource() + "\nProvide OIDC-token or subject-ID, domain, fnr and " +
                    " name of credential resource.");
        }
    }

    public XacmlRequest getRequest() {
        return xacmlRequest;
    }


    public static XacmlRequest getEmptyRequest() {
        return new XacmlRequest().withRequest(new Request());

    }
}
