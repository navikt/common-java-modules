package no.nav.common.rest.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.log.MDCConstants;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.MDC;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.common.log.LogFilter.CONSUMER_ID_HEADER_NAME;
import static no.nav.common.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
import static no.nav.common.utils.EnvironmentUtils.NAIS_APP_NAME_PROPERTY_NAME;

public class RestClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void client_skal_legge_pa_headers() throws IOException {
        OkHttpClient client = RestClient.baseClient();

        MDC.put(MDCConstants.MDC_CALL_ID, "CALL_ID");
        System.setProperty(NAIS_APP_NAME_PROPERTY_NAME, "test");

        Request request = new Request.Builder()
                .url("http://localhost:" + wireMockRule.port())
                .build();

        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(200)));
        
        try (Response ignored = client.newCall(request).execute()) {
            verify(getRequestedFor(anyUrl())
                    .withHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, equalTo("CALL_ID"))
                    .withHeader("Nav-CallId", equalTo("CALL_ID"))
                    .withHeader("X-Correlation-Id", equalTo("CALL_ID"))
                    .withHeader(CONSUMER_ID_HEADER_NAME, equalTo("test")));
        }

        MDC.remove(MDCConstants.MDC_CALL_ID);
    }

    @Test
    public void client_skal_legge_pa_call_id_header_med_job_id() throws IOException {
        OkHttpClient client = RestClient.baseClient();

        MDC.put(MDCConstants.MDC_JOB_ID, "JOB_ID");
        System.setProperty(NAIS_APP_NAME_PROPERTY_NAME, "test");

        Request request = new Request.Builder()
                .url("http://localhost:" + wireMockRule.port())
                .build();

        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        try (Response ignored = client.newCall(request).execute()) {
            verify(getRequestedFor(anyUrl())
                    .withHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, equalTo("JOB_ID")));
        }

        MDC.remove(MDCConstants.MDC_JOB_ID);
    }

    @Test
    public void client_skal_legge_pa_ny_call_id_hvis_call_id_mangler() throws IOException {
        OkHttpClient client = RestClient.baseClient();

        MDC.remove(MDCConstants.MDC_CALL_ID);
        System.setProperty(NAIS_APP_NAME_PROPERTY_NAME, "test");

        Request request = new Request.Builder()
                .url("http://localhost:" + wireMockRule.port())
                .build();

        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        var idPattern = matching("^[a-z0-9]{30}(\\w*)$");

        try (Response ignored = client.newCall(request).execute()) {
            verify(getRequestedFor(anyUrl())
                    .withHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, idPattern)
                    .withHeader("Nav-CallId", idPattern)
                    .withHeader("X-Correlation-Id", idPattern));
        }

    }

}
