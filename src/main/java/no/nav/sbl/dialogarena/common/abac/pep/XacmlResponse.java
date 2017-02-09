package no.nav.sbl.dialogarena.common.abac.pep;


import java.util.List;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XacmlResponse response1 = (XacmlResponse) o;

        return response != null ? response.equals(response1.response) : response1.response == null;

    }

    @Override
    public int hashCode() {
        return response != null ? response.hashCode() : 0;
    }
}
