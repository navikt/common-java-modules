package no.nav.common.abac.domain.request;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class XacmlRequest {

    private Request request;

    public Request getRequest() {
        return request;
    }

    public XacmlRequest withRequest(Request request) {
        this.request = request;
        return this;
    }

}
