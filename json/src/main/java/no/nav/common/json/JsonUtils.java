package no.nav.common.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.util.List;

import static com.fasterxml.jackson.databind.type.CollectionType.construct;

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

    @SneakyThrows
    public static <T> T fromJson(String json, TypeReference<T> type) {
        return objectMapper.readValue(json, type);
    }

    @SneakyThrows
    public static <T> T fromJson(InputStream inputStream, TypeReference<T> type) {
        return objectMapper.readValue(inputStream, type);
    }

    @SneakyThrows
    public static <T> List<T> fromJsonArray(String json, Class<T> valueClass) {
        return objectMapper.readValue(json, listType(valueClass));
    }
    @SneakyThrows
    public static <T> List<T> fromJsonArray(InputStream inputStream, Class<T> valueClass) {
        return objectMapper.readValue(inputStream, listType(valueClass));
    }

    private static <T> CollectionType listType(Class<T> valueClass) {
        return construct(List.class, objectMapper.getTypeFactory().constructType(valueClass));
    }

}
