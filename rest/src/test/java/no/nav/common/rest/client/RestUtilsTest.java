package no.nav.common.rest.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static java.util.function.Predicate.isEqual;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
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
        // Arrange
        var client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();
        String json = "{ \"hello\": \"world\" }";

        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(json)));

        var request = new Request.Builder().url(baseUrl).build();

        // Act
        try (Response response = client.newCall(request).execute()) {
            Optional<String> body = RestUtils.getBodyStr(response);

            // Assert
            Assertions.assertThat(body).isPresent().get().isEqualTo(json);
        }
    }

    @SneakyThrows
    @Test
    public void parseJsonResponse__skal_deserialisere_body() {
        // Arrange
        var expected = new HelloWorldPayload();
        expected.hello = "world";

        var client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();
        String json = "{ \"hello\": \"world\" }";

        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(json)));

        var request = new Request.Builder().url(baseUrl).build();

        // Act
        try (Response response = client.newCall(request).execute()) {
            Optional<HelloWorldPayload> body = RestUtils.parseJsonResponse(response, HelloWorldPayload.class);

            // Assert
            Assertions.assertThat(body).isPresent().get().isEqualTo(expected);
        }
    }

    @SneakyThrows
    @Test
    public void parseJsonResponseOrThrow__kaster_exception_ved_tom_json_body() {
        // Arrange
        var client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();
        String json = "";

        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(json)));

        var request = new Request.Builder().url(baseUrl).build();

        // Act & Assert
        try (Response response = client.newCall(request).execute()) {
            assertThrows(
                    IllegalStateException.class,
                    () -> RestUtils.parseJsonResponseOrThrow(response, HelloWorldPayload.class));
        }
    }

    @SneakyThrows
    @Test
    public void parseJsonArrayResponse__skal_deserialisere_body() {
        // Arrange
        var expected = new HelloWorldPayload();
        expected.hello = "world";

        var client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();
        String json = "[{\"hello\": \"world\"}, {\"hello\": \"world\"}]";

        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(json)));

        var request = new Request.Builder().url(baseUrl).build();

        // Act
        try (Response response = client.newCall(request).execute()) {
            Optional<List<HelloWorldPayload>> body =
                    RestUtils.parseJsonArrayResponse(response, HelloWorldPayload.class);

            // Assert
            Assertions.assertThat(body)
                    .isPresent()
                    .get()
                    .asInstanceOf(list(HelloWorldPayload.class))
                    .hasSize(2)
                    .allMatch(isEqual(expected));
        }
    }

    @SneakyThrows
    @Test
    public void parseJsonArrayResponse__kaster_exception_ved_tom_body() {
        // Arrange
        var client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();
        String json = "";

        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(json)));

        var request = new Request.Builder().url(baseUrl).build();

        // Act & Assert
        try (Response response = client.newCall(request).execute()) {
            assertThrows(
                    IllegalStateException.class,
                    () -> RestUtils.parseJsonResponseArrayOrThrow(response, HelloWorldPayload.class));
        }
    }

    @SneakyThrows
    @Test
    public void getBodyStr__skal_returnere_empty_body_ved_204() {
        // Arrange
        var client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(204)));

        var request = new Request.Builder().url(baseUrl).build();

        // Act
        try (Response response = client.newCall(request).execute()) {
            boolean empty = RestUtils.getBodyStr(response).isEmpty();

            // Assert
            assertTrue(empty);
        }
    }

    @SneakyThrows
    @Test
    public void getBodyStr__skal_returnere_empty_body_for_tom_body() {
        // Arrange
        var client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(204).withBody("")));

        var request = new Request.Builder().url(baseUrl).build();

        // Act
        try (Response response = client.newCall(request).execute()) {
            boolean empty = RestUtils.getBodyStr(response).isEmpty();

            // Assert
            assertTrue(empty);
        }
    }

    @SneakyThrows
    @Test
    public void throwIfNotSuccessful__skal_kaste_exception_ved_feil() {
        // Arrange
        var client = new OkHttpClient.Builder().build();
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(404).withBody("")));

        var request = new Request.Builder().url(baseUrl).build();

        // Act & Assert
        try (Response response = client.newCall(request).execute()) {
            assertThrows(RuntimeException.class, () -> RestUtils.throwIfNotSuccessful(response));
        }
    }

    @Test
    public void toJsonRequestBody_skal_serialisere_request_body() throws IOException {
        // Arrange
        var payload = new HelloWorldPayload();
        String expected = "{\"hello\":\"world\"}";

        payload.hello = "world";

        // Act
        RequestBody jsonRequestBody = RestUtils.toJsonRequestBody(payload);

        // Assert
        Assertions.assertThat(jsonRequestBody.contentLength()).isEqualTo(expected.length());
    }

    @Test
    public void createBearerToken_simply_suffixes_token_with_Bearer() {
        // Arrange: testdata er inline.
        // Act & Assert
        Assertions.assertThat(RestUtils.createBearerToken("Token")).isEqualTo("Bearer Token");
    }
}
