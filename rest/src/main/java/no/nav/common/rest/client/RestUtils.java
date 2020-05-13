package no.nav.common.rest.client;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Optional;

@Slf4j
public class RestUtils {

    private static final Gson gson = new Gson();

    public static MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    public static void throwIfNotSuccessful(Response response) {
        if (!response.isSuccessful()) {
            String message = String.format("Uventet status %d ved kall mot mot %s", response.code(), response.request().url().toString());
            log.error(message);
            throw new RuntimeException(message);
        }
    }

    public static Optional<String> getBodyStr(ResponseBody body) throws IOException {
        if (body == null) {
            return Optional.empty();
        }

        return Optional.of(body.string());
    }

    public static <T> Optional<T> parseJsonResponseBody(ResponseBody body, Class<T> classOfT) throws IOException {
        return getBodyStr(body).map(bodyStr -> gson.fromJson(bodyStr, classOfT));
    }

    public static <T> Optional<T> parseJsonResponseBody(ResponseBody body, Type type) throws IOException {
        return getBodyStr(body).map(bodyStr -> gson.fromJson(bodyStr, type));
    }

    public static <T> T parseJsonResponseBodyOrThrow(ResponseBody body, Class<T> classOfT) throws IOException {
        return parseJsonResponseBody(body, classOfT)
                .orElseThrow(() -> new IllegalStateException("Unable to parse JSON from request body"));
    }

    public static <T> T parseJsonResponseBodyOrThrow(ResponseBody body, Type type) throws IOException {
        return (T) parseJsonResponseBody(body, type)
                .orElseThrow(() -> new IllegalStateException("Unable to parse JSON from request body"));
    }

    public static RequestBody toJsonRequestBody(Object toJson) {
        return RequestBody.create(MEDIA_TYPE_JSON, gson.toJson(toJson));
    }

}
