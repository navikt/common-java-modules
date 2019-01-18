package no.nav.apiapp.ioc;

import no.nav.fo.apiapp.JettyTest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IocTest extends JettyTest {

    @Test
    public void ioc() {
        assertThat(getString("/api/ioc")).isEqualTo(uri("/api/ioc").toString());
    }

}
