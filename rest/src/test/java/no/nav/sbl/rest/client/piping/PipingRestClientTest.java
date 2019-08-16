package no.nav.sbl.rest.client.piping;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.sbl.rest.client.PipingRestClient;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PipingRestClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);

    @Test
    public void getList() {
        PipingRestClient fasitClient = new PipingRestClient(this::httpServletRequestProvider, "http://localhost:" + wireMockRule.port());

        String json = "[{\"name\": \"veilarbaktivitet\"}]";

        givenThat(get(urlEqualTo("/conf/applications"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)));

        List<TestDTO> testDTOS = fasitClient.request("/conf/applications").getList(TestDTO.class);
        verify(getRequestedFor(urlMatching("/conf/applications")));
        assertThat(testDTOS).isNotEmpty();
        assertThat(testDTOS.stream().map(d -> d.name)).contains("veilarbaktivitet");
    }

    private HttpServletRequest httpServletRequestProvider() {
        return httpServletRequest;
    }

    private static class TestDTO {
        @SuppressWarnings("unused")
        private String name;
    }

}
