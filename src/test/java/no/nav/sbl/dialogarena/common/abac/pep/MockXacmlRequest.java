package no.nav.sbl.dialogarena.common.abac.pep;


public class MockXacmlRequest {
    public static XacmlRequest getXacmlRequest() {
        return new XacmlRequest().withRequest(getRequest());
    }

    public static XacmlRequest getXacmlRequestWithSubjectAttributes() {
        return new XacmlRequest().withRequest(getRequestWithSubjectAttributes());
    }

    public static XacmlRequest getXacmlRequestWithSubjAttrWithoutEnvironment() {
        return new XacmlRequest().withRequest(getRequestWithSubjAttrWithoutEnvironment());
    }

    private static Request getRequestWithActionAndResource() {
        final Action action = new Action();
        action.getAttribute().add(new Attribute("urn:oasis:names:tc:xacml:1.0:action:action-id", "read"));

        final Resource resource = new Resource();
        resource.getAttribute().add(new Attribute("no.nav.abac.attributter.resource.felles.resource_type", "no.nav.abac.attributter.resource.felles.person"));
        resource.getAttribute().add(new Attribute("no.nav.abac.attributter.resource.felles.domene", "Foreldrepenger"));
        resource.getAttribute().add(new Attribute("no.nav.abac.attributter.resource.felles.person.fnr", "01010122222"));

        return new Request()
                .withAction(action)
                .withResource(resource);
    }

    private static Request getRequest() {
        final Environment environment = new Environment();
        environment.getAttribute().add(new Attribute("no.nav.abac.attributter.environment.felles.oidc_token_body", "eyJpc3MiOiJuYXYubm8iLCJleHAiOjE0ODQ2NTI2NzIsImp0aSI6IkZHdXJVYWdleFRwTUVZTjdMRHlsQ1EiLCJpYXQiOjE0ODQ2NTIwNzIsIm5iZiI6MTQ4NDY1MTk1Miwic3ViIjoiYTExMTExMSJ9"));
        environment.getAttribute().add(new Attribute("no.nav.abac.attributter.environment.felles.pep_id", "srvEksempelPep"));

        return getRequestWithActionAndResource().withEnvironment(environment);
    }

    private static Request getRequestWithSubjectAttributes() {
        final Environment environment = new Environment();
        environment.getAttribute().add(new Attribute("no.nav.abac.attributter.environment.felles.pep_id", "srvEksempelPep"));

        final AccessSubject accessSubject = new AccessSubject();
        accessSubject.getAttribute().add(new Attribute("urn:oasis:names:tc:xacml:1.0:subject:subject-id", "A111111"));
        accessSubject.getAttribute().add(new Attribute("no.nav.abac.attributter.subject.felles.subjectType", "InternBruker"));

        return getRequestWithActionAndResource()
                .withAccessSubject(accessSubject)
                .withEnvironment(environment);
    }

    private static Request getRequestWithSubjAttrWithoutEnvironment() {
        final AccessSubject accessSubject = new AccessSubject();
        accessSubject.getAttribute().add(new Attribute("urn:oasis:names:tc:xacml:1.0:subject:subject-id", "A111111"));
        accessSubject.getAttribute().add(new Attribute("no.nav.abac.attributter.subject.felles.subjectType", "InternBruker"));

        return getRequestWithActionAndResource().withAccessSubject(accessSubject);
    }
}
