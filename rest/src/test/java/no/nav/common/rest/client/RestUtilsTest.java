package no.nav.common.rest.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RestUtilsTest {

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
            assertEquals(json, RestUtils.getBodyStr(response).get());
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

}
