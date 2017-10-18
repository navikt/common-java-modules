package no.nav.json;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

import java.util.Collections;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.json.DateConfiguration.dateModule;
import static no.nav.sbl.util.EnvironmentUtils.EnviromentClass.T;
import static no.nav.sbl.util.EnvironmentUtils.isEnvironmentClass;

@Produces({"*/*", APPLICATION_JSON})
@Consumes({"*/*", APPLICATION_JSON})
public class JsonProvider extends JacksonJaxbJsonProvider {

    public JsonProvider() {
        setMapper(createObjectMapper());
    }

    public JsonProvider(List<Module> modules) {
        ObjectMapper objectMapper = createObjectMapper();
        modules.forEach(objectMapper::registerModule);
        setMapper(objectMapper);
    }

    public static JsonProvider jsonProviderWithKotlin() {
        return new JsonProvider(singletonList(new KotlinModule()));
    }

    public static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new Jdk8Module())
                .registerModule(dateModule())
                .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                ;

        if (isEnvironmentClass(T)) {
            objectMapper.configure(INDENT_OUTPUT, true);
        }

        objectMapper.setVisibilityChecker(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(ANY)
                .withGetterVisibility(NONE)
                .withSetterVisibility(NONE)
                .withCreatorVisibility(NONE)
        );

        return objectMapper;
    }

}
