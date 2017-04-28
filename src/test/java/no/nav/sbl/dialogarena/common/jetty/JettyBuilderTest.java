package no.nav.sbl.dialogarena.common.jetty;

import org.eclipse.jetty.jaas.JAASLoginService;
import org.junit.Test;

import javax.jms.JMSException;
import java.io.File;
import java.util.Map;

import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class JettyBuilderTest {

    @Test
    public void startJetty() throws JMSException {
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

}
