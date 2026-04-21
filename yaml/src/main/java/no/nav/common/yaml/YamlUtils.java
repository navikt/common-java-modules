package no.nav.common.yaml;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import lombok.SneakyThrows;
import no.nav.common.json.JsonMapper;

public class YamlUtils {

    private static final ObjectMapper objectMapper = JsonMapper.applyDefaultConfiguration(new ObjectMapper(new YAMLFactory()));

    @SneakyThrows
    public static <T> T fromYaml(String yaml, Class<T> targetClass) {
        return objectMapper.readValue(yaml, targetClass);
    }

    @SneakyThrows
    public static String toYaml(Object value) {
        return objectMapper.writeValueAsString(value);
    }

}
