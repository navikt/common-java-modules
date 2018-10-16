package no.nav.fo.apiapp.rest;

import no.nav.fo.apiapp.JettyTest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerTest extends JettyTest {

    @Test
    public void get() {
        assertThat(getString("/api/server")).hasSize(31_000);
    }

}
