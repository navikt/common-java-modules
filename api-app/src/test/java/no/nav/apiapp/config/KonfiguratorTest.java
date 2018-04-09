package no.nav.apiapp.config;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.TestContext;
import no.nav.sbl.dialogarena.common.jetty.Jetty.JettyBuilder;
import org.junit.Test;

import static no.nav.sbl.dialogarena.test.SystemProperties.setTemporaryProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KonfiguratorTest {

    static {
        TestContext.setup();
    }

    private JettyBuilder jettyBuilder = mock(JettyBuilder.class);
    private ApiApplication apiApplication = mock(ApiApplication.class);

    @Test
    public void defaultStsConfig_() {
        when(apiApplication.getApplicationName()).thenReturn("testapp");

        setTemporaryProperty("SECURITYTOKENSERVICE_URL", "test-url", () -> {
            setTemporaryProperty("SRVTESTAPP_USERNAME", "username", () -> {
                setTemporaryProperty("SRVTESTAPP_PASSWORD", "password", () -> {

                    StsConfig stsConfig = new Konfigurator(jettyBuilder, apiApplication).defaultStsConfig();
                    assertThat(stsConfig.url).isEqualTo("test-url");
                    assertThat(stsConfig.username).isEqualTo("username");
                    assertThat(stsConfig.password).isEqualTo("password");

                });
            });
        });
    }

}