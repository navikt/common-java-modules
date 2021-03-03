package no.nav.common.abac;

import no.nav.common.abac.constants.NavAttributter;
import no.nav.common.abac.constants.StandardAttributter;
import no.nav.common.abac.domain.Attribute;
import no.nav.common.abac.domain.request.*;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;

import static no.nav.common.abac.constants.AbacDomain.MODIA_DOMAIN;
import static no.nav.common.abac.constants.AbacDomain.VEILARB_DOMAIN;

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

    public static AccessSubject lagVeilederAccessSubject(NavIdent veilederIdent) {
        AccessSubject accessSubject = new AccessSubject();
        accessSubject.getAttribute().add(new Attribute(StandardAttributter.SUBJECT_ID, veilederIdent.get()));
        accessSubject.getAttribute().add(new Attribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, "InternBruker"));
        return accessSubject;
    }

    public static Action lagAction(ActionId actionId) {
        Action action = new Action();
        action.getAttribute().add(new Attribute(StandardAttributter.ACTION_ID, actionId.getId()));
        return action;
    }

    public static Environment lagEnvironment(String srvUsername) {
        Environment environment = new Environment();
        environment.getAttribute().add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, srvUsername));
        return environment;
    }

    public static Environment lagEnvironmentMedOidcTokenBody(String srvUsername, String oidcTokenBody) {
        Environment environment = lagEnvironment(srvUsername);
        environment.getAttribute().add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, oidcTokenBody));
        return environment;
    }

    public static Resource lagEnhetResource(EnhetId enhetId, String domain) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_FELLES_ENHET));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, domain));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_ENHET, enhetId.get()));
        return resource;
    }

    public static Resource lagEnhetMedSperreResource(EnhetId enhetId, String domain) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_VEILARB_ENHET_EIENDEL));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, domain));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_VEILARB_KONTOR_LAAS, enhetId.get()));
        return resource;
    }

    public static Resource lagPersonResource(EksternBrukerId eksternBrukerId, String domain) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_FELLES_PERSON));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, domain));
        resource.getAttribute().add(personIdAttribute(eksternBrukerId));
        return resource;
    }

    public static Resource lagKode7Resource(String domain) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_KODE_7));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, domain));
        return resource;

    }

    public static Resource lagKode6Resource(String domain) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_KODE_6));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, domain));
        return resource;
    }

    public static Resource lagEgenAnsattResource(String domain) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_EGEN_ANSATT));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, domain));
        return resource;
    }

    public static Resource lagVeilArbResource(String domain) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_VEILARB));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, domain));
        return resource;
    }

    @SuppressWarnings("unused")
    public static Resource lagVeilArbPersonResource(EksternBrukerId eksternBrukerId, String domain) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_VEILARB_PERSON));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, domain));
        resource.getAttribute().add(personIdAttribute(eksternBrukerId));
        return resource;
    }

    @SuppressWarnings("unused")
    public static Resource lagVeilArbUnderOppfolgingResource(EksternBrukerId eksternBrukerId, String domain) {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_VEILARB_UNDER_OPPFOLGING));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, domain));
        resource.getAttribute().add(personIdAttribute(eksternBrukerId));
        return resource;
    }

    public static Resource lagOppfolgingDomeneResource() {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_VEILARB));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, VEILARB_DOMAIN));
        return resource;
    }

    public static Resource lagModiaDomeneResource() {
        Resource resource = new Resource();
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_MODIA));
        resource.getAttribute().add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, MODIA_DOMAIN));
        return resource;
    }

    public static Attribute personIdAttribute(EksternBrukerId eksternBrukerId) {
        switch (eksternBrukerId.type()) {
            case NORSK_IDENT:
            case FNR:
                return new Attribute(NavAttributter.RESOURCE_FELLES_PERSON_FNR, eksternBrukerId.get());
            case AKTOR_ID:
                return new Attribute(NavAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, eksternBrukerId.get());
            default:
                throw new IllegalStateException("Ukjent verdi for ekstern person id type: " + eksternBrukerId.type());
        }
    }

}
