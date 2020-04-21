package no.nav.common.abac.domain.response;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ResponseTypeAdapter implements JsonDeserializer<List<Response>> {
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
