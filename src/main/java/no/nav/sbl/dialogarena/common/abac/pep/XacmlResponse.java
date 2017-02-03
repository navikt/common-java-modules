package no.nav.sbl.dialogarena.common.abac.pep;


public class XacmlResponse {
    private Response response;

    Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public XacmlResponse withResponse(Response response) {
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
