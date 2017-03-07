package no.nav.sbl.dialogarena.common.abac.pep;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.List;

import static no.nav.sbl.dialogarena.common.abac.pep.Utils.entityToString;


public class XacmlMapper {

    public static XacmlResponse mapRawResponse(HttpResponse rawResponse) {
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
        Type attributeAssignmentType = new TypeToken<List<AttributeAssignment>>() {
        }.getType();
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                .setPrettyPrinting()
                .registerTypeAdapter(responseType, new ResponseTypeAdapter())
                .registerTypeAdapter(associatedAdviceType, new AssociatedAdviceTypeAdapter())
                .registerTypeAdapter(attributeAssignmentType, new AttributeAssignmentTypeAdapter())
                .create();
    }

    public static StringEntity mapRequestToEntity(XacmlRequest request) {
        final Gson gson = getGson();
        StringEntity postingString;
        try {
            postingString = new StringEntity(gson.toJson(request));

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Cannot parse object to json request. " + e.getMessage());
        }
        return postingString;
    }
}
