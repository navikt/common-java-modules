package no.nav.common.token_client.test_utils;

import static java.lang.String.format;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mockwebserver3.MockResponse;

public class RequestUtils {

    public static Map<String, List<String>> parseFormData(String formData) {
        Map<String, List<String>> data = new HashMap<>();
        String[] values = formData.split("&");

        for (String value : values) {
            String[] keyValue = value.split("=", 2);
            String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            String decodedValue = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
            data.computeIfAbsent(key, ignored -> new ArrayList<>()).add(decodedValue);
        }

        return data;
    }

    public static MockResponse tokenMockResponse(String accessToken) {
        String body = format("{ \"token_type\": \"Bearer\", \"access_token\": \"%s\", \"expires\": 3600 }", accessToken);

        return new MockResponse.Builder()
            .body(body)
            .addHeader("Content-Type", "application/json")
            .build();
    }
}
