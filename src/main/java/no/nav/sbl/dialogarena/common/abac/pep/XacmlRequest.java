package no.nav.sbl.dialogarena.common.abac.pep;


import com.google.gson.annotations.SerializedName;

public class XacmlRequest {

    @SerializedName("Request")
    private Request request;

    XacmlRequest withRequest(Request request) {
        this.request = request;
        return this;
    }
}
