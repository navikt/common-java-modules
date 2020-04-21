package no.nav.common.abac.domain.response;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AttributeAssignmentTypeAdapter implements JsonDeserializer<List<AttributeAssignment>> {
    @Override
    public List<AttributeAssignment> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        List<AttributeAssignment> attributeAssignments = new ArrayList<>();
        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                attributeAssignments.add(context.deserialize(element, AttributeAssignment.class));
            }
        } else if (json.isJsonObject()) {
            attributeAssignments.add(context.deserialize(json, AttributeAssignment.class));
        } else {
            throw new RuntimeException("Unexpected JSON type: " + json.getClass());
        }
        return attributeAssignments;
    }
}
