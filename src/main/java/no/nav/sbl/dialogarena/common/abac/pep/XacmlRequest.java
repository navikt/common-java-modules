package no.nav.sbl.dialogarena.common.abac.pep;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class XacmlRequest {

    private Request request;

    XacmlRequest withRequest(Request request) {
        this.request = request;
        return this;
    }

}
