package no.nav.sbl.dialogarena.common.abac.pep;


class Request {

    private AccessSubject accessSubject;
    private Environment environment;
    private Action action;
    private Resource resource;

    public Environment getEnvironment() {
        return environment;
    }

    public AccessSubject getAccessSubject() { return accessSubject; }

    public Action getAction() {
        return action;
    }

    public Resource getResource() {
        return resource;
    }

    Request withEnvironment(Environment environment) {
        this.environment = environment;
        return this;
    }

    Request withAccessSubject(AccessSubject accessSubject) {
        this.accessSubject = accessSubject;
        return this;
    }

    Request withAction(Action action) {
        this.action = action;
        return this;
    }

    Request withResource(Resource resource) {
        this.resource = resource;
        return this;
    }
}
