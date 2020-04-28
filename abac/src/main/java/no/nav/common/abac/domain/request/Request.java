package no.nav.common.abac.domain.request;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@Getter
@ToString
public class Request {

    private AccessSubject accessSubject;
    private Environment environment;
    private Action action;
    private Resource resource;

    public Request withEnvironment(Environment environment) {
        this.environment = environment;
        return this;
    }

    public Request withAccessSubject(AccessSubject accessSubject) {
        this.accessSubject = accessSubject;
        return this;
    }

    public Request withAction(Action action) {
        this.action = action;
        return this;
    }

    public Request withResource(Resource resource) {
        this.resource = resource;
        return this;
    }

}