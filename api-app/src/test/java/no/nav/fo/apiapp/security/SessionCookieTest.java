package no.nav.fo.apiapp.security;

import no.nav.fo.apiapp.JettyTest;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;


public class SessionCookieTest extends JettyTest {

    private static final String KREVER_SESJON_API = "/api/session";

    @Test
    public void endpoint_creating_session__fails() throws InterruptedException {
        Response response = get(KREVER_SESJON_API);
        assertThat(response.getStatus()).isEqualTo(500);
    }

}
