package no.nav.common.token_client.test_utils;

import okhttp3.mockwebserver.MockResponse;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class RequestUtils {

    public static Map<String, String> parseFormData(String formData) {
        Map<String, String> data = new HashMap<>();
        String[] values = formData.split("&");

        Arrays.stream(values).forEach(v -> {
            String[] keyValue = v.split("=");
            data.put(keyValue[0], URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
        });

        return data;
    }

    public static MockResponse tokenMockResponse(String accessToken) {
        String body = format("{ \"token_type\": \"Bearer\", \"access_token\": \"%s\", \"expires\": 3600 }", accessToken);

        return new MockResponse()
             .setBody(body)
             .setHeader("Content-Type", "application/json");
    }

}
