package no.nav.common.abac.domain.response;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CategoryTypeAdapter implements JsonDeserializer<List<Category>> {
    @Override
    public List<Category> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        List<Category> categories = new ArrayList<>();
        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                categories.add(context.deserialize(element, Category.class));
            }
        } else if (json.isJsonObject()) {
            categories.add(context.deserialize(json, Category.class));
        } else {
            throw new RuntimeException("Unexpected JSON type: " + json.getClass());
        }
        return categories;
    }
}
