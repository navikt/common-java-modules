package no.nav.sbl.rest.client.piping;

import no.nav.sbl.rest.client.PipingRestClient;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PipingRestClientTest {
    private HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    private PipingRestClient fasitClient = new PipingRestClient(this::httpServletRequestProvider, "https://fasit.adeo.no");

    @Test
    public void getList() {
        List<TestDTO> testDTOS = fasitClient.request("/conf/applications").getList(TestDTO.class);
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