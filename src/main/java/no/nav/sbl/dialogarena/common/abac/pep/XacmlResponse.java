package no.nav.sbl.dialogarena.common.abac.pep;


import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode
public class XacmlResponse {
    private List<Response> response;

    public List<Response> getResponse() {
        return response;
    }

    public void setResponse(List<Response> response) {
        this.response = response;
    }

    public XacmlResponse withResponse(List<Response> response) {
        this.response = response;
        return this;
    }
}
