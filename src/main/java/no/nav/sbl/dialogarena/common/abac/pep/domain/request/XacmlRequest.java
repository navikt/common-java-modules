package no.nav.sbl.dialogarena.common.abac.pep.domain.request;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class XacmlRequest {

    private Request request;

    public XacmlRequest withRequest(Request request) {
        this.request = request;
        return this;
    }

}
