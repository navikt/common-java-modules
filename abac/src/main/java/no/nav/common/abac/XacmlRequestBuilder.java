package no.nav.common.abac;

import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.Attribute;
import no.nav.common.abac.domain.request.*;

public class XacmlRequestBuilder {

    public static XacmlRequest buildRequest(Environment environment, Action action, AccessSubject accessSubject, Resource resource) {
        return new XacmlRequest()
                .withRequest(
                        new Request()
                            .withEnvironment(environment)
                            .withAction(action)
                            .withAccessSubject(accessSubject)
                            .withResource(resource)
                );
    }

    public static AccessSubject lagVeilederAccessSubject(String veilederIdent) {
        return lagAccessSubject(veilederIdent, "InternBruker");
    }

    public static AccessSubject lagAccessSubject(String subjectId, String subjectType) {
        AccessSubject accessSubject = new AccessSubject();
        accessSubject.getAttribute().add(new Attribute(StandardAttributter.SUBJECT_ID, subjectId));
        accessSubject.getAttribute().add(new Attribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, subjectType));
        return accessSubject;
    }

    public static Action lagAction(Action.ActionId actionId) {
        Action action = new Action();
        action.getAttribute().add(new Attribute(StandardAttributter.ACTION_ID, actionId.getId()));
        return action;
    }

    public static Environment lagEnvironment(String srvUsername) {
        Environment environment = new Environment();
        environment.getAttribute().add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, srvUsername));
        return environment;
    }

    public static Resource lagEnhetResource(String enhetId, String domain) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_VEILARB_ENHET_EIENDEL));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, domain));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_VEILARB_KONTOR_LAAS, enhetId));
        return resource;
    }

    public static Resource lagPersonResource(AbacPersonId personId, String domain) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_FELLES_PERSON));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, domain));
        resource.getAttribute().add(personIdAttribute(personId));
        return resource;
    }

    public static Resource lagKode7Resource(RequestData requestData) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_KODE_7));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, requestData.getDomain()));
        return resource;

    }

    public static Resource lagKode6Resource(RequestData requestData) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_KODE_6));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, requestData.getDomain()));
        return resource;
    }

    public static Resource lagEgenAnsattResource(RequestData requestData) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_EGEN_ANSATT));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, requestData.getDomain()));
        return resource;
    }

    public static Resource lagVeilArbResource(RequestData requestData) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_VEILARB));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, requestData.getDomain()));
        return resource;
    }

    public static Resource lagVeilArbPersonResource(RequestData requestData) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_VEILARB_PERSON));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, requestData.getDomain()));
        resource.getAttribute().add(personIdAttribute(requestData.getPersonId()));
        return resource;
    }

    public static Resource lagVeilArbUnderOppfolgingResource(RequestData requestData) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_VEILARB_UNDER_OPPFOLGING));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, requestData.getDomain()));
        resource.getAttribute().add(personIdAttribute(requestData.getPersonId()));
        return resource;
    }

    public static Attribute personIdAttribute(AbacPersonId personId) {
        switch (personId.getType()) {
            case FNR:
                return new Attribute(NavAttributter.RESOURCE_FELLES_PERSON_FNR, personId.getId());
            case AKTOR_ID:
                return new Attribute(NavAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, personId.getId());
            default:
                throw new IllegalStateException("Ukjent verdi for person id type: " + personId.getType());
        }
    }

}
