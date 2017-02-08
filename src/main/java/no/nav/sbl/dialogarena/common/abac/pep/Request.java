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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Request)) return false;

        Request request = (Request) o;

        if (accessSubject != null ? !accessSubject.equals(request.accessSubject) : request.accessSubject != null)
            return false;
        if (environment != null ? !environment.equals(request.environment) : request.environment != null) return false;
        if (action != null ? !action.equals(request.action) : request.action != null) return false;
        return resource != null ? resource.equals(request.resource) : request.resource == null;
    }

    @Override
    public int hashCode() {
        int result = accessSubject != null ? accessSubject.hashCode() : 0;
        result = 31 * result + (environment != null ? environment.hashCode() : 0);
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (resource != null ? resource.hashCode() : 0);
        return result;
    }
}
