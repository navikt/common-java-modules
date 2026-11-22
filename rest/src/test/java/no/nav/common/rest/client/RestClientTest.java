package no.nav.common.rest.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static no.nav.common.rest.filter.LogRequestFilter.NAV_CALL_ID_HEADER_NAME;
import static no.nav.common.rest.filter.LogRequestFilter.NAV_CONSUMER_ID_HEADER_NAME;
import static no.nav.common.utils.EnvironmentUtils.NAIS_APP_NAME_PROPERTY_NAME;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import no.nav.common.log.MDCConstants;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.MDC;

public class RestClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private Properties originalProperties;
    private Map<String, String> originalContextMap;

    @Before
    public void setUp() {
        originalProperties = (Properties) System.getProperties().clone();
        originalContextMap = MDC.getCopyOfContextMap();
        MDC.clear();
    }

    @After
    public void tearDown() {
        try {
            System.setProperties(originalProperties);
        } finally {
            if (originalContextMap == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(originalContextMap);
            }
        }
    }

    @Test
    public void client_skal_legge_pa_headers() throws IOException {
        OkHttpClient client = RestClient.baseClient();

        MDC.put(MDCConstants.MDC_CALL_ID, "CALL_ID");
        System.setProperty(NAIS_APP_NAME_PROPERTY_NAME, "test");

        Request request = new Request.Builder()
                .url("http://localhost:" + wireMockRule.port())
                .build();

        wireMockRule.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        try (Response ignored = client.newCall(request).execute()) {
            wireMockRule.verify(getRequestedFor(anyUrl())
                    .withHeader(NAV_CALL_ID_HEADER_NAME, equalTo("CALL_ID"))
                    .withHeader(NAV_CONSUMER_ID_HEADER_NAME, equalTo("test")));
        }
    }

    @Test
    public void client_skal_legge_pa_call_id_header_med_job_id() throws IOException {
        OkHttpClient client = RestClient.baseClient();

        MDC.put(MDCConstants.MDC_JOB_ID, "JOB_ID");
        System.setProperty(NAIS_APP_NAME_PROPERTY_NAME, "test");

        Request request = new Request.Builder()
                .url("http://localhost:" + wireMockRule.port())
                .build();

        wireMockRule.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        try (Response ignored = client.newCall(request).execute()) {
            wireMockRule.verify(getRequestedFor(anyUrl()).withHeader(NAV_CALL_ID_HEADER_NAME, equalTo("JOB_ID")));
        }
    }

    @Test
    public void client_skal_legge_pa_ny_call_id_hvis_call_id_mangler() throws IOException {
        OkHttpClient client = RestClient.baseClient();

        MDC.remove(MDCConstants.MDC_CALL_ID);
        System.setProperty(NAIS_APP_NAME_PROPERTY_NAME, "test");

        Request request = new Request.Builder()
                .url("http://localhost:" + wireMockRule.port())
                .build();

        wireMockRule.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        var idPattern = matching("^[a-z0-9]{30}(\\w*)$");

        try (Response ignored = client.newCall(request).execute()) {
            wireMockRule.verify(getRequestedFor(anyUrl()).withHeader(NAV_CALL_ID_HEADER_NAME, idPattern));
        }
    }
}
