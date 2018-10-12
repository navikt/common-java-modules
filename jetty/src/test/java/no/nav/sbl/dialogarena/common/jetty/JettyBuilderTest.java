package no.nav.sbl.dialogarena.common.jetty;

import org.eclipse.jetty.jaas.JAASLoginService;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class JettyBuilderTest {

    @Before
    public void setup() {
        TestListener.reset();
    }

    @Test
    public void startJetty() {
        Jetty jetty = usingWar(new File("src/test/webapp"))
                .at("contextpath")
                .port(8888)
                .overrideWebXml()
                .withLoginService(new JAASLoginService())
                .buildJetty();

        jetty.start();
        jetty.stop.run();

        Map<String, String> contextParams = TestListener.getContextParams();
        assertThat(contextParams.get("webxml-param"),equalTo("webxml-param"));
        assertThat(contextParams.get("override-webxml-param"),equalTo("override-webxml-param"));
    }

    @Test
    public void startJettyWithoutAnnotationScanning() {
        Jetty jetty = usingWar(new File("src/test/webapp"))
                .at("contextpath")
                .port(8888)
                .overrideWebXml()
                .disableAnnotationScanning()
                .withLoginService(new JAASLoginService())
                .buildJetty();

        jetty.start();
        jetty.stop.run();

        assertThat(TestListener.getContextParams(), nullValue());
    }

    @Test
    public void startJettyWithoutStatistics() {
        Jetty jetty = usingWar(new File("src/test/webapp"))
                .at("contextpath")
                .port(8888)
                .overrideWebXml()
                .disableStatistics()
                .withLoginService(new JAASLoginService())
                .buildJetty();

        jetty.start();
        jetty.stop.run();
    }

    @Test
    public void restartJetty() throws Exception {
        Jetty jetty = usingWar(new File("src/test/webapp"))
                .at("contextpath")
                .port(8888)
                .overrideWebXml()
                .withLoginService(new JAASLoginService())
                .buildJetty();

        jetty.start();
        jetty.stop.run();

        jetty.start();
        jetty.server.stop();

        jetty.server.start();
        jetty.server.stop();

        jetty.start();
        jetty.stop.run();
    }

}
