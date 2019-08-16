package no.nav.fo.apiapp.rest;

import no.nav.fo.apiapp.JettyTest;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class RedirectTest extends JettyTest {

    @Test
    public void get() {
        Response response = getResponse();
        assertThat(response.getStatus(), equalTo(307));
        URI location = response.getLocation();
        assertThat(location.getScheme(), equalTo("testscheme"));
    }

    private Response getResponse() {
        String path = "/api/redirect";
        return request(path, builder -> builder.header("X-Forwarded-Proto","testscheme").get());
    }

}
