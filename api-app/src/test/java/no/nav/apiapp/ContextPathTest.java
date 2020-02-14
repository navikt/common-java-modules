package no.nav.apiapp;

import no.nav.fo.apiapp.JettyTest;
import no.nav.sbl.rest.RestUtils;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.function.Consumer;

import static no.nav.fo.apiapp.rest.JettyTestUtils.getHostName;
import static no.nav.log.LogFilter.CONSUMER_ID_HEADER_NAME;
import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class ContextPathTest extends JettyTest {

    @Test
    public void pingIsPublic() {
        assertThat(getWithContextPath(APPLICATION_NAME + "/api/ping")).satisfies(status(200));
        assertThat(getWithContextPath(APPLICATION_NAME + "/api/foo")).satisfies(status(404));
    }

    private Consumer<Response> status(int status) {
        return response -> assertThat(response.getStatus()).isEqualTo(status);
    }

    private Response getWithContextPath(String path) {
        UriBuilder uriBuilder = UriBuilder.fromPath(path).host(getHostName()).scheme("https").port(getSslPort());
        return RestUtils.withClient(c -> c.target(uriBuilder)
                .request()
                .header(PREFERRED_NAV_CALL_ID_HEADER_NAME, "callId")
                .header(CONSUMER_ID_HEADER_NAME, "consumerId")
                .get()
        );
    }
}
