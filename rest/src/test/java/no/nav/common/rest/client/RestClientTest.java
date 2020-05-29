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
    }

}
