package no.nav.sbl.dialogarena.common.abac;

import com.google.gson.*;
import no.nav.sbl.dialogarena.common.abac.pep.Response;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

class ResponseTypeAdapter implements JsonDeserializer<List<Response>> {
    @Override
    public List<Response> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        List<Response> responses = new ArrayList<>();
        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                responses.add(context.deserialize(element, Response.class));
            }
        } else if (json.isJsonObject()) {
            responses.add(context.deserialize(json, Response.class));
        } else {
            throw new RuntimeException("Unexpected JSON type: " + json.getClass());
        }
        return responses;
    }
}
