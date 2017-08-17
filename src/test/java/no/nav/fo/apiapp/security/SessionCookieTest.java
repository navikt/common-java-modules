package no.nav.fo.apiapp.security;

import no.nav.fo.apiapp.JettyTest;
import org.junit.Test;

import javax.ws.rs.core.NewCookie;

import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


public class SessionCookieTest extends JettyTest {

    private static final String SESJONS_COOKIE_NAVN = CONTEXT_NAME.toUpperCase() + "_JSESSIONID";
    private static final String KREVER_SESJON_API = "/api/session";

    @Test
    public void sikkerCookie() {
        NewCookie sesjonsCookie = getOgFinnSesjonsCookie();
        assertThat(sesjonsCookie, notNullValue());
        assertThat(sesjonsCookie.isHttpOnly(), is(true));
        assertThat(sesjonsCookie.isSecure(), is(true));
    }

    @Test
    public void sesjonsTimeout() throws InterruptedException {
        String sesjonsId = getString(KREVER_SESJON_API);
        assertThat(sesjonsId, equalTo(getString(KREVER_SESJON_API)));
        sleep(10000L);
        assertThat(sesjonsId, not(equalTo(getString(KREVER_SESJON_API))));
    }

    private NewCookie getOgFinnSesjonsCookie() {
        get(KREVER_SESJON_API);
        return getCookies().get(SESJONS_COOKIE_NAVN);
    }

}
