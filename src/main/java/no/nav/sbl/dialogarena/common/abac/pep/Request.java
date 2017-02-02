package no.nav.sbl.dialogarena.common.abac.pep;


class Request {

    private Environment environment;
    private Action action;
    private Resource resource;

    public Environment getEnvironment() {
        return environment;
    }

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

    Request withAction(Action action) {
        this.action = action;
        return this;
    }

    Request withResource(Resource resource) {
        this.resource = resource;
        return this;
    }
}
