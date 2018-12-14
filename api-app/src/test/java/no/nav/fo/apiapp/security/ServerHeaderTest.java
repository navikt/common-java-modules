package no.nav.fo.apiapp.security;

import no.nav.fo.apiapp.JettyTest;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static no.nav.apiapp.ApiAppServletContextListener.INTERNAL_IS_ALIVE;
import static org.assertj.core.api.Assertions.assertThat;


public class ServerHeaderTest extends JettyTest {

    @Test
    public void endpoint_creating_session__fails() {
        Response response = get(INTERNAL_IS_ALIVE);
        assertThat(response.getHeaderString("Server")).isEqualTo(APPLICATION_NAME);
    }

}
