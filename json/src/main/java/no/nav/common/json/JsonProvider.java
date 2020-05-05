package no.nav.common.json;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Produces({"*/*", APPLICATION_JSON})
@Consumes({"*/*", APPLICATION_JSON})
public class JsonProvider extends JacksonJaxbJsonProvider {

    public JsonProvider() {
        this(createObjectMapper());
    }

    public JsonProvider(ObjectMapper objectMapper) {
        setMapper(objectMapper);
    }

    @Deprecated
    public JsonProvider(List<Module> modules) {
        ObjectMapper objectMapper = createObjectMapper();
        modules.forEach(objectMapper::registerModule);
        setMapper(objectMapper);
    }

    public static ObjectMapper createObjectMapper() {
        return applyDefaultConfiguration(new ObjectMapper());
    }

    public static ObjectMapper applyDefaultConfiguration(ObjectMapper objectMapper) {
        objectMapper.registerModule(new Jdk8Module())
                .registerModule(DateConfiguration.dateModule())
                .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        objectMapper.setVisibilityChecker(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(ANY)
                .withGetterVisibility(NONE)
                .withSetterVisibility(NONE)
                .withCreatorVisibility(NONE)
        );

        return objectMapper;
    }

}
