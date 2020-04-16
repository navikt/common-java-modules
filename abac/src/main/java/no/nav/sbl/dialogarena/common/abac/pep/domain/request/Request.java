package no.nav.sbl.dialogarena.common.abac.pep.domain.request;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@EqualsAndHashCode
@Getter
@ToString
public class Request {

    private AccessSubject accessSubject;
    private Environment environment;
    private Action action;
    private List<Resource> resource;

    public Optional<Resource> getFirstResource() {
        if (resource == null || resource.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(resource.get(0));
    }

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

    public Request withResources(List<Resource> resource) {
        this.resource = resource;
        return this;
    }

    public Request withResource(Resource resource) {
        return withResources(Arrays.asList(resource));
    }

}
