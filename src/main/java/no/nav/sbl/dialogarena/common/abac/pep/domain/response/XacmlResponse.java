package no.nav.sbl.dialogarena.common.abac.pep.domain.response;


import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode
public class XacmlResponse {
    private List<Response> response;

    private boolean fallbackUsed = false;

    public List<Response> getResponse() {
        return response;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public void setResponse(List<Response> response) {
        this.response = response;
    }

    public XacmlResponse withResponse(List<Response> response) {
        this.response = response;
        return this;
    }

    public XacmlResponse withFallbackUsed() {
        fallbackUsed = true;
        return this;
    }
}
