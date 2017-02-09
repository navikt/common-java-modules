package no.nav.sbl.dialogarena.common.abac;

import com.google.gson.*;
import no.nav.sbl.dialogarena.common.abac.pep.Advice;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

class AssociatedAdviceTypeAdapter implements JsonDeserializer<List<Advice>> {
    @Override
    public List<Advice> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        List<Advice> associatedAdvice = new ArrayList<>();
        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                associatedAdvice.add(context.deserialize(element, Advice.class));
            }
        } else if (json.isJsonObject()) {
            associatedAdvice.add(context.deserialize(json, Advice.class));
        } else {
            throw new RuntimeException("Unexpected JSON type: " + json.getClass());
        }
        return associatedAdvice;
    }
}
