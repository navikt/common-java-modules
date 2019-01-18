package no.nav.apiapp.config;

import no.nav.apiapp.TestContext;
import no.nav.sbl.dialogarena.common.jetty.Jetty.JettyBuilder;
import no.nav.sbl.dialogarena.test.junit.SystemPropertiesRule;
import org.junit.Rule;
import org.junit.Test;

import static no.nav.sbl.util.EnvironmentUtils.APP_NAME_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class KonfiguratorTest {

    static {
        TestContext.setup();
    }

    private JettyBuilder jettyBuilder = mock(JettyBuilder.class);

    @Rule
    public SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Test
    public void defaultStsConfig() {
        systemPropertiesRule.setProperty(APP_NAME_PROPERTY_NAME, "testapp")
                .setProperty("SECURITYTOKENSERVICE_URL", "test-url")
                .setProperty("SRVTESTAPP_USERNAME", "username")
                .setProperty("SRVTESTAPP_PASSWORD", "password");

        StsConfig stsConfig = new Konfigurator(jettyBuilder).defaultStsConfig();
        assertThat(stsConfig.url).isEqualTo("test-url");
        assertThat(stsConfig.username).isEqualTo("username");
        assertThat(stsConfig.password).isEqualTo("password");
    }

}