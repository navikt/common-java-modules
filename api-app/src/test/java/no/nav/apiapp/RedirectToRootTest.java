package no.nav.apiapp;

import no.nav.fo.apiapp.JettyTest;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import static no.nav.sbl.rest.RestUtils.withClient;
import static org.assertj.core.api.Assertions.assertThat;

public class RedirectToRootTest extends JettyTest {

    @Test
    public void redirectToRoot(){
        withClient(client -> {
            UriBuilder uriBuilder = UriBuilder.fromPath("/").host("localhost").port(getSslPort()).scheme("https");
            Response response = client.target(uriBuilder).request().get();
            assertThat(response.getStatus()).isEqualTo(302);
            assertThat(response.getLocation()).isEqualTo(uriBuilder.path("api-app").build());
            return response;
        });
    }

}
