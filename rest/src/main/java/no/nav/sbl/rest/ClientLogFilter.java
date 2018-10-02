package no.nav.sbl.rest;

import no.nav.log.LogFilter;
import no.nav.log.MDCConstants;
import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.MDC;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

import static java.util.stream.Collectors.joining;
import static javax.ws.rs.core.HttpHeaders.COOKIE;
import static no.nav.sbl.util.ListUtils.mutableList;
import static no.nav.sbl.util.StringUtils.of;
import static org.slf4j.LoggerFactory.getLogger;

public class ClientLogFilter implements ClientResponseFilter, ClientRequestFilter {

    private static final Logger LOG = getLogger(RestUtils.class);
    private static final String NAME = ClientLogFilter.class.getName();
    private static final String CSRF_TOKEN = "csrf-token";

    private final String metricName;
    private final boolean disableParameterLogging;
    private final boolean useMetrics;

    ClientLogFilter(String metricName, boolean disableParameterLogging, boolean useMetrics) {
        this.metricName = metricName;
        this.disableParameterLogging = disableParameterLogging;
        this.useMetrics = useMetrics;
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        LOG.info("{} {}", clientRequestContext.getMethod(), uriForLogging(clientRequestContext));

        MultivaluedMap<String, Object> requestHeaders = clientRequestContext.getHeaders();

        of(MDC.get(MDCConstants.MDC_CORRELATION_ID))
                .ifPresent(correlationId -> requestHeaders.add(LogFilter.CORRELATION_ID_HEADER_NAME, correlationId));

        requestHeaders.add(RestUtils.CSRF_COOKIE_NAVN, CSRF_TOKEN);
        requestHeaders.add(COOKIE, new Cookie(RestUtils.CSRF_COOKIE_NAVN, CSRF_TOKEN));


        // jersey-client generates cookies in org.glassfish.jersey.message.internal.CookieProvider according to the
        // deprecated rfc2109 specification, which prefixes the cookie with its version. This may not be supported by modern servers.
        // Therefore we serialize cookies on the more modern and simpler rfc6265-format
        // https://www.ietf.org/rfc/rfc2109.txt
        // https://tools.ietf.org/html/rfc6265
        requestHeaders.replace(COOKIE, mutableList(requestHeaders.get(COOKIE)
                .stream()
                .map(this::toCookieString)
                .collect(joining("; "))
        ));


        if (useMetrics) {
            Timer timer = MetricsFactory.createTimer(this.metricName);
            timer.start();
            clientRequestContext.setProperty(NAME, timer);
        }
    }

    private String toCookieString(Object cookie) {
        if (cookie instanceof String) {
            return (String) cookie;
        } else if (cookie instanceof Cookie) {
            Cookie c = (Cookie) cookie;
            return c.getName() + "=" + c.getValue();
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext) throws IOException {
        if (useMetrics) {
            Timer timer = (Timer) clientRequestContext.getProperty(NAME);
            timer
                    .stop()
                    .addFieldToReport("httpStatus", clientResponseContext.getStatus())
                    .addFieldToReport("host", clientRequestContext.getUri().getHost())
                    .addFieldToReport("path", clientRequestContext.getUri().getPath())
                    .report();
        }
    }

    private URI uriForLogging(ClientRequestContext clientRequestContext) {
        URI uri = clientRequestContext.getUri();
        return disableParameterLogging ? UriBuilder.fromUri(uri).replaceQuery("").build() : uri;
    }

}
