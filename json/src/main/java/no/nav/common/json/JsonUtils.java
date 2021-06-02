package no.nav.common.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.util.List;

public class JsonUtils {

    private static final ObjectMapper objectMapper = JsonMapper.defaultObjectMapper();

    public static ObjectMapper getMapper() {
        return objectMapper;
    }

    @SneakyThrows
    public static String toJson(Object obj) {
        return obj != null ? objectMapper.writeValueAsString(obj) : null;
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

    private static CollectionType listType(Class<?> valueClass) {
        return objectMapper.getTypeFactory().constructCollectionType(List.class, valueClass);
    }

}
