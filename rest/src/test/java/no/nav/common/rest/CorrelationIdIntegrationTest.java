package no.nav.common.rest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import no.nav.common.test.junit.SystemPropertiesRule;
import no.nav.common.utils.EnvironmentUtils;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.matching.RequestPattern.everything;
import static java.util.Arrays.stream;
import static no.nav.common.log.LogFilter.CONSUMER_ID_HEADER_NAME;
import static no.nav.common.log.LogFilter.NAV_CALL_ID_HEADER_NAMES;
import static no.nav.common.log.MDCConstants.MDC_CALL_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class CorrelationIdIntegrationTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Rule
    public SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Test
    public void smoketest() {
        givenThat(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("ok!"))
        );

        getRequest();

        String correlationId = CorrelationIdIntegrationTest.class.getName();
        String applicationName = CorrelationIdIntegrationTest.class.getSimpleName();
        MDC.put(MDC_CALL_ID, correlationId);
        systemPropertiesRule.setProperty(EnvironmentUtils.NAIS_APP_NAME_PROPERTY_NAME, applicationName);

        getRequest();

        List<LoggedRequest> requests = wireMockRule.findRequestsMatching(everything()).getRequests();
        assertThat(requests).hasSize(2);

        LoggedRequest firstRequest = requests.get(0);
        assertThat(firstRequest.getHeader(CONSUMER_ID_HEADER_NAME)).isNull();
        stream(NAV_CALL_ID_HEADER_NAMES).forEach(h-> assertThat(firstRequest.getHeader(h)).isNull());

        LoggedRequest secondRequest = requests.get(1);
        assertThat(secondRequest.getHeader(CONSUMER_ID_HEADER_NAME)).isEqualTo(applicationName);
        stream(NAV_CALL_ID_HEADER_NAMES).forEach(h-> assertThat(secondRequest.getHeader(h)).isEqualTo(correlationId));
    }

    private void getRequest() {
        RestUtils.withClient(c -> c.target("http://localhost:" + wireMockRule.port()).request().get(String.class));
    }

}