package no.nav.apiapp;

import no.nav.fo.apiapp.ApplicationConfigWithoutContextPath;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.rest.RestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.util.function.Consumer;

import static no.nav.fo.apiapp.JettyTest.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ContextPathTest {

    private static Jetty jetty;

    @BeforeClass
    public static void start() {
        jetty = nyJettyForTest(ApplicationConfigWithoutContextPath.class);
    }

    @AfterClass
    public static void stopJetty() {
        jetty.stop.run();
    }

    @Test
    public void pingIsPublic() {
        assertThat(get("/ping")).satisfies(status(200));
        assertThat(get("/some/path")).satisfies(status(401));
    }

    private Consumer<Response> status(int status) {
        return response -> assertThat(response.getStatus()).isEqualTo(status);
    }

    private static Response get(String path) {
        UriBuilder uriBuilder = UriBuilder.fromPath(path).host("localhost").scheme("https").port(getSslPort(jetty));
        return RestUtils.withClient(c -> c.target(uriBuilder).request().get());
    }

}
