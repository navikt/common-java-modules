package no.nav.fo.apiapp.rest;

import no.nav.fo.apiapp.JettyTest;
import org.junit.Test;

import static javax.ws.rs.core.HttpHeaders.USER_AGENT;
import static org.assertj.core.api.Assertions.assertThat;

public class ServerTest extends JettyTest {

    @Test
    public void get() {
        assertThat(getString("/api/server")).hasSize(31_000);

        assertThat(target("/api/server").request()
                .cookie("LARGE_COOKIE", ServerEksempel.string(15))
                .header(USER_AGENT, "curl")
                .get()
                .getStatus()
        ).isEqualTo(200);
    }

}
