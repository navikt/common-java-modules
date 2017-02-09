package no.nav.sbl.dialogarena.common.abac.pep.domain.response;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ResponseTypeAdapter implements JsonDeserializer<List<Response>> {
    @Override
    public List<Response> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
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
