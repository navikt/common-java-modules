package no.nav.dialogarena.mock;

import lombok.SneakyThrows;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.URLResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

// TODO
public class MockHandler extends ResourceHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MockServer.class);
    private static final String PATH_PATH = "/mockserver";

    private final String contextPath;
    private final AtomicLong requestCounter = new AtomicLong();

    public MockHandler(String contextName) {
        this.contextPath = contextName.startsWith("/") ? contextName : "/" + contextName;
        this.setBaseResource(URLResource.newClassPathResource(PATH_PATH + this.contextPath));
    }

    @Override
    public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        long requestNumber = requestCounter.incrementAndGet();
        if (contextPath.equals(target)) {
            Response response = baseRequest.getResponse();
            response.setContentType("text/plain");
            response.getWriter().write("mock for: " + contextPath);
            baseRequest.setHandled(true);
        } else if (target.startsWith(this.contextPath + "/internal/selftest")) {
            httpServletResponse.getWriter().write("OK");
            baseRequest.setHandled(true);
        } else {
            if (getResource("/") == null) {
                throw new IOException("Unable to locate /mockserver in resources. Check your configuration.");
            }

            String pathInfo = target.substring(this.contextPath.length()) + "." + baseRequest.getMethod();
            String jsPath = pathInfo + ".js";
            String jsonPath = pathInfo + ".json";

            Resource javascript = getResource(jsPath);
            if (javascript.exists()) {
                JavascriptEngine.evaluateJavascript(javascript, request, httpServletResponse);
                baseRequest.setHandled(true);
                return;
            }

            if (!getResource(pathInfo).exists() && getResource(jsonPath).exists()) {
                pathInfo = jsonPath;
            }
            baseRequest.setMethod(HttpMethod.GET.name());
            baseRequest.setPathInfo(pathInfo);

            super.handle(target, baseRequest, request, httpServletResponse);

            if (baseRequest.isHandled()) {
                LOG.info("request #{} {}{}", requestNumber, PATH_PATH, pathInfo);
            } else {
                LOG.warn("request #{} {}{} -- NOT FOUND", requestNumber, PATH_PATH, pathInfo);
            }
        }
    }

    @SuppressWarnings("unused")
    public long getRequestCount() {
        return requestCounter.get();
    }

}
