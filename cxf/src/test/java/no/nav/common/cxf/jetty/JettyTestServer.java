package no.nav.common.cxf.jetty;

import static java.lang.String.format;
import static java.lang.System.setProperty;
import static org.apache.cxf.staxutils.StaxUtils.ALLOW_INSECURE_PARSER;
import static org.mockito.Mockito.mock;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.cxf.CXFEndpoint;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;

@Slf4j
public abstract class JettyTestServer {

    private static final String SERVICE_PATH = "/test";
    private static final String CONTEXT_PATH = "/" + JettyTestServer.class.getSimpleName();

    private Jetty jetty;

    @SuppressWarnings("SameParameterValue") // Publisert testhjelper støtter også andre serviceklasser.
    protected <T> String startCxfServer(Class<T> serviceClass) throws IOException {
        return startCxfServer(serviceClass, mock(serviceClass));
    }

    protected <T> String startCxfServer(Class<T> serviceClass, T service) throws IOException {
        // Hvis andre tester har opprettet en bus allerede, må denne stoppes først
        BusFactory.getThreadDefaultBus().shutdown(false);

        setProperty(ALLOW_INSECURE_PARSER, Boolean.TRUE.toString());

        int port = findFreePort();
        jetty = Jetty.usingWar().at(CONTEXT_PATH).port(port).buildJetty();
        jetty.context.addServlet(new ServletHolder(new CxfServlet(service)), "/*");
        jetty.start();

        String uri = URI.create(format("http://localhost:%d", port) + CONTEXT_PATH + SERVICE_PATH)
                .toString();

        log.info("{} is running at {}", serviceClass, uri);
        return uri;
    }

    public static int findFreePort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

    @AfterEach
    public void cleanup() throws Exception {
        if (jetty != null) {
            jetty.server.stop();
        }
    }

    private static class CxfServlet extends CXFNonSpringServlet {

        private final Object service;

        private <T> CxfServlet(T service) {
            this.service = service;
        }

        @Override
        public void init(ServletConfig sc) throws ServletException {
            super.init(sc);
            new CXFEndpoint()
                    .serviceBean(service)
                    .disableSAMLIn()
                    .address(SERVICE_PATH)
                    .create();
        }
    }
}
