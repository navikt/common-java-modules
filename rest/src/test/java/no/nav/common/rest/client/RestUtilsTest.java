package no.nav.common.rest.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;


import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.function.Predicate.isEqual;
import static org.junit.Assert.*;

public class RestUtilsTest {

    @Setter
    @Getter
    @EqualsAndHashCode
    static class HelloWorldPayload {
        private String hello;
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);


    @SneakyThrows
    @Test
    public void getBodyStr__skal_returnere_body() {
        OkHttpClient client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();
        String json = "{ \"hello\": \"world\" }";

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        Request request =  new Request.Builder().url(baseUrl).build();

        try (Response response = client.newCall(request).execute()) {
            Assertions.assertThat(RestUtils.getBodyStr(response)).isPresent().get().isEqualTo(json);
        }
    }

    @SneakyThrows
    @Test
    public void parseJsonResponse__skal_deserialisere_body() {

        HelloWorldPayload expected = new HelloWorldPayload();
        expected.hello = "world";

        OkHttpClient client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();
        String json = "{ \"hello\": \"world\" }";

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        Request request =  new Request.Builder().url(baseUrl).build();

        try (Response response = client.newCall(request).execute()) {
            Assertions.assertThat(RestUtils.parseJsonResponse(response, HelloWorldPayload.class)).isPresent().get().isEqualTo(expected);
        }
    }

    @SneakyThrows
    @Test
    public void parseJsonResponseOrThrow__kaster_exception_ved_tom_json_body() {

        OkHttpClient client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();
        String json = "";

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        Request request =  new Request.Builder().url(baseUrl).build();

        try (Response response = client.newCall(request).execute()) {
            assertThrows(IllegalStateException.class, () -> RestUtils.parseJsonResponseOrThrow(response, HelloWorldPayload.class));
        }
    }

    @SneakyThrows
    @Test
    public void parseJsonArrayResponse__skal_deserialisere_body() {

        HelloWorldPayload expected = new HelloWorldPayload();
        expected.hello = "world";

        OkHttpClient client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();
        String json = "[{\"hello\": \"world\"}, {\"hello\": \"world\"}]";

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        Request request =  new Request.Builder().url(baseUrl).build();

        try (Response response = client.newCall(request).execute()) {
            Assertions.assertThat(
                    RestUtils.parseJsonArrayResponse(response, HelloWorldPayload.class))
                    .isPresent()
                    .get().asList()
                    .hasSize(2)
                    .allMatch(isEqual(expected));
        }
    }

    @SneakyThrows
    @Test
    public void parseJsonArrayResponse__kaster_exception_ved_tom_body() {

        HelloWorldPayload expected = new HelloWorldPayload();
        expected.hello = "world";

        OkHttpClient client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();
        String json = "";

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        Request request =  new Request.Builder().url(baseUrl).build();

        try (Response response = client.newCall(request).execute()) {
            assertThrows(IllegalStateException.class, () -> RestUtils.parseJsonResponseArrayOrThrow(response, HelloWorldPayload.class));
        }
    }


    @SneakyThrows
    @Test
    public void getBodyStr__skal_returnere_empty_body_for_null() {
        OkHttpClient client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(204))
        );

        Request request =  new Request.Builder().url(baseUrl).build();

        try (Response response = client.newCall(request).execute()) {
            assertTrue(RestUtils.getBodyStr(response).isEmpty());
        }
    }

    @SneakyThrows
    @Test
    public void getBodyStr__skal_returnere_empty_body_for_tom_body() {
        OkHttpClient client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(204)
                        .withBody(""))
        );

        Request request =  new Request.Builder().url(baseUrl).build();

        try (Response response = client.newCall(request).execute()) {
            assertTrue(RestUtils.getBodyStr(response).isEmpty());
        }
    }

    @SneakyThrows
    @Test
    public void throwIfNotSuccessful__skal_kaste_exception_ved_feil() {
        OkHttpClient client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody(""))
        );

        Request request =  new Request.Builder().url(baseUrl).build();

        try (Response response = client.newCall(request).execute()) {
            assertThrows(RuntimeException.class, () -> RestUtils.throwIfNotSuccessful(response));
        }
    }

    @Test
    public void toJsonRequestBody_skal_serialisere_request_body() throws IOException {
        HelloWorldPayload payload = new HelloWorldPayload();
        String expected = "{\"hello\":\"world\"}";

        payload.hello = "world";
        RequestBody jsonRequestBody = RestUtils.toJsonRequestBody(payload);
        Assertions.assertThat(jsonRequestBody.contentLength()).isEqualTo(expected.length());
    }

    @Test
    public void createBearerToken_simply_suffixes_token_with_Bearer() {
        Assertions.assertThat(RestUtils.createBearerToken("Token")).isEqualTo("Bearer Token");
    }

    @SneakyThrows
    @Test
    public void parseJsonResponseWithValueArrayOrThrow_skal_deserialisere_body() {
        // Arrange
        HelloWorldPayload expected = new HelloWorldPayload();
        expected.hello = "world";

        OkHttpClient client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();
        String json = "{\"value\": [{\"hello\": \"world\"}, {\"hello\": \"world\"}]}";

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        Request request = new Request.Builder().url(baseUrl).build();

        // Act & Assert
        try (Response response = client.newCall(request).execute()) {
            List<HelloWorldPayload> result = RestUtils.parseJsonResponseWithValueArrayOrThrow(response, HelloWorldPayload.class);
            Assertions.assertThat(result)
                    .hasSize(2)
                    .allMatch(isEqual(expected));
        }
    }

    @SneakyThrows
    @Test
    public void parseJsonResponseWithValueArrayOrThrow_skal_kaste_exception_ved_null_body() {
        OkHttpClient client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(204))
        );

        Request request = new Request.Builder().url(baseUrl).build();

        try (Response response = client.newCall(request).execute()) {
            assertThrows(IllegalStateException.class, () ->
                    RestUtils.parseJsonResponseWithValueArrayOrThrow(response, HelloWorldPayload.class));
        }
    }

    @SneakyThrows
    @Test
    public void parseJsonResponseWithValueArrayOrThrow_skal_kaste_exception_ved_tom_body() {
        OkHttpClient client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{}"))
        );

        Request request = new Request.Builder().url(baseUrl).build();

        try (Response response = client.newCall(request).execute()) {
            assertThrows(IllegalStateException.class, () ->
                    RestUtils.parseJsonResponseWithValueArrayOrThrow(response, HelloWorldPayload.class));
        }
    }

    @SneakyThrows
    @Test
    public void parseJsonResponseWithValueArrayOrThrow_skal_kaste_exception_uten_value_felt() {
        OkHttpClient client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();
        String json = "{\"data\": [{\"hello\": \"world\"}]}";

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        Request request = new Request.Builder().url(baseUrl).build();

        try (Response response = client.newCall(request).execute()) {
            assertThrows(IllegalStateException.class, () ->
                    RestUtils.parseJsonResponseWithValueArrayOrThrow(response, HelloWorldPayload.class));
        }
    }

    @SneakyThrows
    @Test
    public void parseJsonResponseWithValueArrayOrThrow_skal_kaste_exception_naar_value_ikke_er_array() {
        OkHttpClient client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();
        String json = "{\"value\": \"not an array\"}";

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        Request request = new Request.Builder().url(baseUrl).build();

        try (Response response = client.newCall(request).execute()) {
            assertThrows(IllegalStateException.class, () ->
                    RestUtils.parseJsonResponseWithValueArrayOrThrow(response, HelloWorldPayload.class));
        }
    }

    @SneakyThrows
    @Test
    public void parseJsonResponseWithValueArrayOrThrow_skal_kaste_exception_ved_ugyldig_json() {
        OkHttpClient client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();
        String json = "{invalid json";

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        Request request = new Request.Builder().url(baseUrl).build();

        try (Response response = client.newCall(request).execute()) {
            assertThrows(Exception.class, () ->
                    RestUtils.parseJsonResponseWithValueArrayOrThrow(response, HelloWorldPayload.class));
        }
    }

}
