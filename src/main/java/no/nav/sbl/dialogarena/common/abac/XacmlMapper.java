package no.nav.sbl.dialogarena.common.abac;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import no.nav.sbl.dialogarena.common.abac.pep.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.List;

import static no.nav.sbl.dialogarena.common.abac.pep.xacml.Utils.entityToString;


class XacmlMapper {

    static XacmlResponse mapRawResponse(HttpResponse rawResponse) {
        final HttpEntity httpEntity = rawResponse.getEntity();
        final String content = entityToString(httpEntity);
        Gson gson = getGsonForResponse();

        return gson.fromJson(content, XacmlResponse.class);
    }

    private static Gson getGson() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                .setPrettyPrinting()
                .create();
    }

    private static Gson getGsonForResponse() {
        Type responseType = new TypeToken<List<Response>>() {
        }.getType();
        Type associatedAdviceType = new TypeToken<List<Advice>>() {
        }.getType();
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                .setPrettyPrinting()
                .registerTypeAdapter(responseType, new ResponseTypeAdapter())
                .registerTypeAdapter(associatedAdviceType, new AssociatedAdviceTypeAdapter())
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
