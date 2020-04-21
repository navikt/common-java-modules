package no.nav.common.jetty;

import no.nav.common.rest.RestUtils;
import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletContextListener;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.Map;

import static no.nav.common.jetty.Jetty.usingWar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
        assertThat(contextParams.get("webxml-param")).isEqualTo("webxml-param");
        assertThat(contextParams.get("override-webxml-param")).isEqualTo("override-webxml-param");
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

        assertThat(TestListener.getContextParams()).isNull();
    }


    @Test
    public void startJettyWithCustomization() {
        ServletContextListener servletContextListener = mock(ServletContextListener.class);
        RequestLog requestLog = mock(RequestLog.class);

        Jetty jetty = usingWar(new File("src/test/webapp"))
                .at("contextpath")
                .port(8888)
                .addCustomizer(new JettyCustomizer() {

                    @Override
                    public void customize(HttpConfiguration httpConfiguration) {
                        httpConfiguration.setSendXPoweredBy(true);
                    }

                    @Override
                    public void customize(Server server) {
                        server.setRequestLog(requestLog);
                    }

                    @Override
                    public void customize(WebAppContext webAppContext) {
                        webAppContext.addEventListener(servletContextListener);

                    }
                })
                .buildJetty();

        jetty.start();

        RestUtils.withClient(c -> {
            Response response = c.target("http://localhost:8888/not/found").request().get();
            assertThat(response.getStatus()).isEqualTo(404);
            verify(requestLog).log(any(), any());
            return null;
        });

        jetty.stop.run();

        verify(servletContextListener).contextInitialized(any());
        verify(servletContextListener).contextDestroyed(any());
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
