package no.nav.dialogarena.mock;

import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.URLResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MockServer {

    private static final Logger LOG = LoggerFactory.getLogger(MockServer.class);
    private static final String CLASS_PATH_PATH = "/mockserver";

    public static Jetty startMockServer(String contextName, int jettyPort) {
        final Jetty jetty = Jetty.usingWar()
                .at(contextName)
                .port(jettyPort)
                .overrideWebXml()
                .buildJetty();
        jetty.server.setHandler(new MockHandler(contextName));
        jetty.start();
        return jetty;
    }

    private static class MockHandler extends ResourceHandler {

        private final String contextPath;

        private MockHandler(String contextName) {
            this.contextPath = "/" + contextName;
            this.setBaseResource(URLResource.newClassPathResource(CLASS_PATH_PATH));
        }

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse httpServletResponse) throws IOException, ServletException {
            if (contextPath.equals(target)) {
                Response response = baseRequest.getResponse();
                response.setContentType("text/plain");
                response.getWriter().write("mock for: " + contextPath);
                baseRequest.setHandled(true);
            } else {
                String pathInfo = target + "." + baseRequest.getMethod();
                baseRequest.setPathInfo(pathInfo);
                baseRequest.setMethod(HttpMethod.GET.name());

                super.handle(target, baseRequest, request, httpServletResponse);

                if (baseRequest.isHandled()) {
                    LOG.info("classpath:{}{}", CLASS_PATH_PATH, pathInfo);
                } else {
                    LOG.warn("classpath:{}{} -- NOT FOUND", CLASS_PATH_PATH, pathInfo);
                }
            }
        }
    }

}
