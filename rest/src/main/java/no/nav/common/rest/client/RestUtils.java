package no.nav.common.rest.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

@Slf4j
public class RestUtils {

    public static MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    public static String createBearerToken(String token) {
        return "Bearer " + token;
    }

    public static void throwIfNotSuccessful(Response response) {
        if (!response.isSuccessful()) {
            String message = String.format("Uventet status %d ved kall mot mot %s", response.code(), response.request().url());
            log.warn(message);
            throw new RuntimeException(message);
        }
    }

    public static Optional<String> getBodyStr(Response response) throws IOException {
        ResponseBody body = response.body();

        if (body == null) {
            return empty();
        }

        String bodyStr = body.string();

        return bodyStr.isBlank() ? empty() : of(bodyStr);
    }

    public static <T> Optional<T> parseJsonResponse(Response response, Class<T> classOfT) throws IOException {
        return getBodyStr(response).map(bodyStr -> JsonUtils.fromJson(bodyStr, classOfT));
    }

    public static <T> Optional<List<T>> parseJsonArrayResponse(Response response, Class<T> classOfT) throws IOException {
        return getBodyStr(response).map(bodyStr -> JsonUtils.fromJsonArray(bodyStr, classOfT));
    }

    public static <T> T parseJsonResponseOrThrow(Response response, Class<T> classOfT) throws IOException {
        return parseJsonResponse(response, classOfT)
                .orElseThrow(() -> new IllegalStateException("Unable to parse JSON object from response body"));
    }

    public static <T> List<T> parseJsonResponseArrayOrThrow(Response response, Class<T> classOfT) throws IOException {
        return parseJsonArrayResponse(response, classOfT)
                .orElseThrow(() -> new IllegalStateException("Unable to parse JSON array from response body"));
    }

    public static <T> List<T> parseJsonResponseWithValueArrayOrThrow(Response response, Class<T> classOfT) throws IOException {
        if (response.body() == null) {
            throw new IllegalStateException("Response body is null for request: " + response.request().url());
        }

        String responseBody = response.body().string();

        // Handle empty response body
        if (responseBody.isBlank() || responseBody.equals("{}")) {
            throw new IllegalStateException("Empty response body for request: " + response.request().url());
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseBody);

            JsonNode valueNode = rootNode.get("value");
            if (valueNode == null || !valueNode.isArray()) {
                throw new IllegalStateException("Missing or invalid 'value' array in response");
            }

            return mapper.readValue(valueNode.toString(),
                    mapper.getTypeFactory().constructCollectionType(List.class, classOfT));
        } catch (Exception e) {
            log.error("Failed to parse response body", e);
            throw e;
        }
    }

    public static RequestBody toJsonRequestBody(Object obj) {
        return RequestBody.create(MEDIA_TYPE_JSON, JsonUtils.toJson(obj));
    }

}
