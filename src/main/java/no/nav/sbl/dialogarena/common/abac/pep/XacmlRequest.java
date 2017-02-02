package no.nav.sbl.dialogarena.common.abac.pep;


import com.google.gson.annotations.SerializedName;

public class XacmlRequest {

    @SerializedName("Request")
    private Request request;

    XacmlRequest withRequest(Request request) {
        this.request = request;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XacmlRequest that = (XacmlRequest) o;

        return request != null ? request.equals(that.request) : that.request == null;

    }

    @Override
    public int hashCode() {
        return request != null ? request.hashCode() : 0;
    }
}
