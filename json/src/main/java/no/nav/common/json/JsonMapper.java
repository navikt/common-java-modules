package no.nav.common.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.module.kotlin.KotlinModule;

public class JsonMapper {

    public static ObjectMapper defaultObjectMapper() {
        return applyDefaultConfiguration(tools.jackson.databind.json.JsonMapper.builder().build());
    }

    public static ObjectMapper applyDefaultConfiguration(ObjectMapper objectMapper) {
        return objectMapper.rebuild()
                .addModule(DateModule.module())
                .addModule(new KotlinModule.Builder().build())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false)
                .changeDefaultVisibility(v -> v
                        .withFieldVisibility(Visibility.ANY)
                        .withGetterVisibility(Visibility.NONE)
                        .withSetterVisibility(Visibility.NONE)
                        .withCreatorVisibility(Visibility.NONE))
                .build();
    }

}
