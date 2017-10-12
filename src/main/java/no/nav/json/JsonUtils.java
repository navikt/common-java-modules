package no.nav.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.InputStream;

public class JsonUtils {

    private static final ObjectMapper objectMapper = JsonProvider.createObjectMapper();

    public static String toJson(Object o) {
        return toJson(o, objectMapper);
    }

    @SneakyThrows
    static String toJson(Object o, ObjectMapper objectMapper) {
        return o != null ? objectMapper.writeValueAsString(o) : null;
    }

    @SneakyThrows
    public static <T> T fromJson(String json, Class<T> valueClass) {
        return objectMapper.readValue(json, valueClass);
    }

    @SneakyThrows
    public static <T> T fromJson(InputStream inputStream, Class<T> valueClass) {
        return objectMapper.readValue(inputStream, valueClass);
    }
}
