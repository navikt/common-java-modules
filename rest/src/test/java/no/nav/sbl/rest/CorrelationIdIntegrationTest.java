package no.nav.sbl.rest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import no.nav.log.MDCConstants;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.matching.RequestPattern.everything;
import static no.nav.log.MDCConstants.MDC_CORRELATION_ID;
import static no.nav.sbl.rest.RestUtils.CORRELATION_ID_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class CorrelationIdIntegrationTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void smoketest() {
        givenThat(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("ok!"))
        );

        getRequest();

        String correlationId = CorrelationIdIntegrationTest.class.getName();
        MDC.put(MDC_CORRELATION_ID, correlationId);

        getRequest();

        List<LoggedRequest> requests = wireMockRule.findRequestsMatching(everything()).getRequests();
        assertThat(requests).hasSize(2);

        LoggedRequest firstRequest = requests.get(0);
        assertThat(firstRequest.containsHeader(CORRELATION_ID_HEADER_NAME)).isFalse();

        LoggedRequest secondReauest = requests.get(1);
        assertThat(secondReauest.getHeader(CORRELATION_ID_HEADER_NAME)).isEqualTo(correlationId);
    }

    private void getRequest() {
        RestUtils.withClient(c -> c.target("http://localhost:" + wireMockRule.port()).request().get(String.class));
    }

}