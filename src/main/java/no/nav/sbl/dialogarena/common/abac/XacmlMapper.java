package no.nav.sbl.dialogarena.common.abac;

import com.google.gson.*;
import no.nav.sbl.dialogarena.common.abac.pep.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.XacmlResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;

import static no.nav.sbl.dialogarena.common.abac.pep.xacml.Utils.entityToString;


public class XacmlMapper {

    static XacmlResponse mapRawResponse(HttpResponse rawResponse) {
        final HttpEntity httpEntity = rawResponse.getEntity();
        final String content = entityToString(httpEntity);
        Gson gson = getGson();
        final XacmlResponse xacmlResponse = gson.fromJson(content, XacmlResponse.class);

        return xacmlResponse;
    }

    private static Gson getGson() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                .setPrettyPrinting()
                .create();
    }

    static StringEntity mapRequestToEntity(XacmlRequest request) {
        final Gson gson = getGson();
        StringEntity postingString = null;
        try {
            postingString = new StringEntity(gson.toJson(request));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return postingString;
    }
}
