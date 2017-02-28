package no.nav.sbl.dialogarena.common.cxf;

import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.slf4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.ServerSocket;

import static java.lang.System.setProperty;
import static org.apache.cxf.staxutils.StaxUtils.ALLOW_INSECURE_PARSER;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;

public abstract class JettyTest {

    private static final Logger LOG = getLogger(JettyTest.class);

    private static final String SERVICE_PATH = "/test";
    private static final String CONTEXT_PATH = "/" + JettyTest.class.getSimpleName();

    private Jetty jetty;

    protected <T> String startCxfServer(Class<T> serviceClass) {
        return startCxfServer(serviceClass, mock(serviceClass));
    }

    protected <T> String startCxfServer(Class<T> serviceClass, T service) {
        try {
            setProperty(ALLOW_INSECURE_PARSER, Boolean.TRUE.toString());

            int port = findFreePort();
            jetty = Jetty.usingWar().at(CONTEXT_PATH).port(port).buildJetty();
            jetty.context.addServlet(new ServletHolder(new CxfServlet(serviceClass, service)), "/*");
            jetty.start();

            String path = new URIBuilder("http://localhost").setPort(port).setPath(CONTEXT_PATH + SERVICE_PATH).build().toString();
            LOG.info("{} is running at {}", serviceClass, path);
            return path;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int findFreePort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

    @After
    public void cleanup() throws Exception {
        if (jetty != null) {
            jetty.server.stop();
        }
    }

    private static class CxfServlet extends CXFNonSpringServlet {

        private final Class<?> serviceClass;
        private final Object service;

        private <T> CxfServlet(Class<T> serviceClass, T service) {
            this.serviceClass = serviceClass;
            this.service = service;
        }

        @Override
        public void init(ServletConfig sc) throws ServletException {
            super.init(sc);
            JaxWsServerFactoryBean jaxWsServerFactoryBean = new JaxWsServerFactoryBean();
            jaxWsServerFactoryBean.setServiceClass(serviceClass);
            jaxWsServerFactoryBean.setServiceBean(service);
            jaxWsServerFactoryBean.setAddress(SERVICE_PATH);
            jaxWsServerFactoryBean.create();
        }
    }

}
