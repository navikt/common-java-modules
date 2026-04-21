package no.nav.common.cxf;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MockHandler extends ResourceHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MockHandler.class);
    private static final String PATH_PATH = "/mockserver";
    private static final List<String> EXTENSIONS = Arrays.asList("json", "xml");

    private final String contextPath;
    private final AtomicLong requestCounter = new AtomicLong();

    public MockHandler(String contextName) {
        this.contextPath = contextName.startsWith("/") ? contextName : "/" + contextName;
        this.setBaseResource(ResourceFactory.of(this).newClassLoaderResource(PATH_PATH + this.contextPath));
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        long requestNumber = requestCounter.incrementAndGet();
        String target = Request.getPathInContext(request);

        if (contextPath.equals(target)) {
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/plain;charset=utf-8");
            Content.Sink.write(response, true, "mock for: " + contextPath, callback);
            return true;
        }

        if (target.startsWith(contextPath + "/internal/selftest")) {
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/plain;charset=utf-8");
            Content.Sink.write(response, true, "OK", callback);
            return true;
        }

        Resource base = getBaseResource();
        if (base == null || !base.exists()) {
            throw new IOException("Unable to locate /mockserver in resources. Check your configuration.");
        }

        String pathInfo = target.substring(contextPath.length()) + "." + request.getMethod();
        Resource resource = base.resolve(pathInfo);

        if (resource == null || !resource.exists()) {
            for (String extension : EXTENSIONS) {
                String candidate = pathInfo + "." + extension;
                Resource withExt = base.resolve(candidate);
                if (withExt != null && withExt.exists()) {
                    pathInfo = candidate;
                    resource = withExt;
                    break;
                }
            }
        }

        if (resource == null || !resource.exists()) {
            LOG.warn("request #{} {}{} -- NOT FOUND", requestNumber, PATH_PATH, pathInfo);
            Response.writeError(request, response, callback, 404);
            return true;
        }

        LOG.info("request #{} {}{}", requestNumber, PATH_PATH, pathInfo);

        String mimeType = MimeTypes.DEFAULTS.getMimeByExtension(pathInfo);
        if (mimeType != null) {
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, mimeType);
        }

        byte[] bytes;
        try (InputStream in = resource.newInputStream()) {
            bytes = in.readAllBytes();
        }
        response.write(true, ByteBuffer.wrap(bytes), callback);
        return true;
    }

    @SuppressWarnings("unused")
    public long getRequestCount() {
        return requestCounter.get();
    }

}
