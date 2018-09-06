package no.nav.common.proxy;

import lombok.extern.slf4j.Slf4j;
import no.nav.log.LogFilter;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.validation.ValidationUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.MDC;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;

import static javax.servlet.DispatcherType.REQUEST;
import static no.nav.log.LogFilter.CORRELATION_ID_HEADER_NAME;
import static no.nav.log.MDCConstants.MDC_CORRELATION_ID;
import static no.nav.log.MDCUtils.withContext;
import static no.nav.sbl.util.StringUtils.of;

@Slf4j
public class ProxyServlet extends org.eclipse.jetty.proxy.ProxyServlet implements Pingable {


    private final String id;
    private final String pingUrl;
    private final Ping.PingMetadata pingMetadata;
    private final ProxyServletConfig proxyServletConfig;
    private final long readTimeout;

    public ProxyServlet(ProxyServletConfig proxyServletConfig) {
        ValidationUtils.validate(proxyServletConfig);
        this.proxyServletConfig = proxyServletConfig;
        this.readTimeout = proxyServletConfig.readTimeout.toMillis();
        this.id = proxyServletConfig.id;
        this.pingUrl = targetUrl(proxyServletConfig.contextPath + proxyServletConfig.pingPath);
        this.pingMetadata = new Ping.PingMetadata(
                "pingMetadata" + id,
                pingUrl,
                "ping backend for " + proxyServletConfig.contextPath,
                false
        );
    }

    @SuppressWarnings("unused")
    public ProxyServletConfig getProxyServletConfig() {
        return proxyServletConfig;
    }

    protected String targetUrl(String requestURI) {
        return proxyServletConfig.baseUrl + requestURI;
    }

    @Override
    protected HttpClient newHttpClient() {
        return new ProxyClient();
    }

    @Override
    protected String rewriteTarget(HttpServletRequest clientRequest) {
        StringBuilder sb = new StringBuilder(targetUrl(clientRequest.getRequestURI()));
        of(clientRequest.getQueryString()).ifPresent(query -> {
            sb.append("?");
            sb.append(query);
        });
        String target = sb.toString();
        log.info("{} {}", clientRequest.getMethod(), target);
        return target;
    }

    @Override
    public HttpClient createHttpClient() throws ServletException {
        HttpClient httpClient = super.createHttpClient();
        httpClient.setIdleTimeout(readTimeout);
        setTimeout(readTimeout + 3000L); // total request-timeout
        return httpClient;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            super.service(request, response);
        } catch (Throwable throwable) {
            log.error(throwable.getMessage(), throwable);
            throw throwable;
        }
    }

    @Override
    protected void addProxyHeaders(HttpServletRequest clientRequest, Request proxyRequest) {
        super.addProxyHeaders(clientRequest, proxyRequest);
        proxyRequest.header(CORRELATION_ID_HEADER_NAME, MDC.get(MDC_CORRELATION_ID));
    }

    @Override
    public Ping ping() {
        try {
            return doPing(pingMetadata);
        } catch (Exception e) {
            return Ping.feilet(pingMetadata, e);
        }
    }

    protected Ping doPing(Ping.PingMetadata pingMetadata) throws Exception {
        ContentResponse contentResponse = newPingRequest().send();
        int status = contentResponse.getStatus();
        if (status == 200) {
            return Ping.lyktes(pingMetadata);
        } else {
            return Ping.feilet(pingMetadata, status + " != 200");
        }
    }

    protected Request newPingRequest() {
        return getHttpClient().newRequest(pingUrl);
    }

    public ServletContextHandler createHandler() {
        ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.addFilter(LogFilter.class, "/*", EnumSet.of(REQUEST));
        servletContextHandler.addServlet(new ServletHolder(this), "/*");
        servletContextHandler.setContextPath(proxyServletConfig.contextPath);
        return servletContextHandler;
    }

    @Override
    protected Response.Listener newProxyResponseListener(HttpServletRequest request, HttpServletResponse response) {
        return new ApplicationProxyResponseListener(request, response, MDC.getCopyOfContextMap());
    }

    private class ApplicationProxyResponseListener extends ProxyResponseListener {

        private final HttpServletResponse response;
        private final Map<String, String> mdcContextMap;

        private ApplicationProxyResponseListener(HttpServletRequest request, HttpServletResponse response, Map<String, String> mdcContextMap) {
            super(request, response);
            this.response = response;
            this.mdcContextMap = mdcContextMap;
        }

        @Override
        public void onComplete(Result result) {
            withContext(mdcContextMap, () -> {

                Response response = result.getResponse();
                int responseStatus = response.getStatus();
                log.info("{} - {}", responseStatus, result.getRequest().getURI());

                if (result.isFailed() || responseStatus >= 300) {
                    this.response.addHeader("Escape-5xx-Redirect", "true");
                    this.response.addHeader("X-Escape-5xx-Redirect", "true");

                    Throwable responseFailure = result.getResponseFailure();
                    if (responseFailure != null) {
                        log.warn(responseFailure.getMessage(), responseFailure);
                    }
                }

            });
            super.onComplete(result);
        }
    }

}
