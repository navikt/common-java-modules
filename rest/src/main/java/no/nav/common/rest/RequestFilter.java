package no.nav.common.rest;

import no.nav.common.log.LogFilter;
import no.nav.common.log.MDCConstants;
import org.slf4j.Logger;
import org.slf4j.MDC;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Collections;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static javax.ws.rs.core.HttpHeaders.COOKIE;
import static no.nav.common.log.LogFilter.NAV_CALL_ID_HEADER_NAMES;
import static no.nav.common.utils.EnvironmentUtils.getApplicationName;
import static no.nav.common.utils.StringUtils.of;
import static org.slf4j.LoggerFactory.getLogger;

public class RequestFilter implements ClientRequestFilter {

    private static final Logger LOG = getLogger(RequestFilter.class);

    private static final String CSRF_TOKEN = "csrf-token";

    @Override
    public void filter(ClientRequestContext requestContext) {
        LOG.info("{} {}", requestContext.getMethod(), requestContext.getUri());

        MultivaluedMap<String, Object> requestHeaders = requestContext.getHeaders();

        of(MDC.get(MDCConstants.MDC_CALL_ID)).ifPresent(callId -> stream(NAV_CALL_ID_HEADER_NAMES).forEach(headerName-> requestHeaders.add(headerName, callId)));
        getApplicationName().ifPresent(applicationName -> requestHeaders.add(LogFilter.CONSUMER_ID_HEADER_NAME, applicationName));

        requestHeaders.add(RestUtils.CSRF_COOKIE_NAVN, CSRF_TOKEN);
        requestHeaders.add(COOKIE, new Cookie(RestUtils.CSRF_COOKIE_NAVN, CSRF_TOKEN));

        // jersey-client generates cookies in org.glassfish.jersey.message.internal.CookieProvider according to the
        // deprecated rfc2109 specification, which prefixes the cookie with its version. This may not be supported by modern servers.
        // Therefore we serialize cookies on the more modern and simpler rfc6265-format
        // https://www.ietf.org/rfc/rfc2109.txt
        // https://tools.ietf.org/html/rfc6265
        requestHeaders.replace(COOKIE, Collections.singletonList((requestHeaders.get(COOKIE))
                .stream()
                .map(this::toCookieString)
                .collect(joining("; "))
        ));

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

}
