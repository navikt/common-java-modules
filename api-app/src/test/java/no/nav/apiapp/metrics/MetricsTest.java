package no.nav.apiapp.metrics;

import no.nav.apiapp.ApiAppServletContextListener;
import no.nav.fo.apiapp.JettyTest;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static no.nav.apiapp.ApiAppServletContextListener.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class MetricsTest extends JettyTest {

    @Test
    public void metrics() {
        sjekkOKStatus(INTERNAL_METRICS);
    }

    private void sjekkOKStatus(String path) {
        Response response = get(path);
        assertThat(response.getStatus(), is(200));
        assertThat(getString(path).toLowerCase(), not(containsString(">error<")));
    }
}
