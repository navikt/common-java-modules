package no.nav.common.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.SneakyThrows;
import no.nav.json.JsonProvider;

public class YamlUtils {

    private static final ObjectMapper objectMapper = JsonProvider.applyDefaultConfiguration(new ObjectMapper(new YAMLFactory()));

    @SneakyThrows
    public static <T> T fromYaml(String yaml, Class<T> targetClass) {
        return objectMapper.readValue(yaml, targetClass);
    }

}
