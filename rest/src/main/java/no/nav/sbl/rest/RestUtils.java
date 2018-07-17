package no.nav.sbl.rest;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.Wither;
import no.nav.json.JsonProvider;
import no.nav.log.MDCConstants;
import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.MDC;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;
import static javax.ws.rs.core.HttpHeaders.COOKIE;
import static no.nav.sbl.util.ListUtils.mutableList;
import static no.nav.sbl.util.StringUtils.of;
import static org.glassfish.jersey.client.ClientProperties.*;
import static org.slf4j.LoggerFactory.getLogger;

public class RestUtils {

    public static final String CORRELATION_ID_HEADER_NAME = "X-Correlation-Id";
    public static final String CSRF_COOKIE_NAVN = "NAV_CSRF_PROTECTION";

    private static final Logger LOG = getLogger(RestUtils.class);

    public static final RestConfig DEFAULT_CONFIG = RestConfig.builder().build();
    public static final RestConfig LONG_READ_CONFIG = DEFAULT_CONFIG.withReadTimeout(DEFAULT_CONFIG.readTimeout * 4);

    @SuppressWarnings("unused")
    public static ClientConfig createClientConfig() {
        return createClientConfig(DEFAULT_CONFIG, getMetricName());
    }

    public static ClientConfig createClientConfig(RestConfig restConfig) {
        return createClientConfig(restConfig, getMetricName());
    }

    private static ClientConfig createClientConfig(RestConfig restConfig, String metricName) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new JsonProvider());
        clientConfig.register(new Filter(metricName, restConfig));
        clientConfig.property(FOLLOW_REDIRECTS, false);
        clientConfig.property(CONNECT_TIMEOUT, restConfig.connectTimeout);
        clientConfig.property(READ_TIMEOUT, restConfig.readTimeout);

        return clientConfig;
    }

    public static Client createClient() {
        return createClient(DEFAULT_CONFIG, getMetricName());
    }

    public static Client createClient(RestConfig restConfig) {
        return createClient(restConfig, getMetricName());
    }

    private static Client createClient(RestConfig restConfig, String metricName) {
        return new JerseyClientBuilder()
                .sslContext(riktigSSLContext())
                .withConfig(createClientConfig(restConfig, metricName))
                .build();
    }

    public static <T> T withClient(Function<Client, T> function) {
        return withClient(DEFAULT_CONFIG, function, getMetricName());
    }

    public static <T> T withClient(RestConfig restConfig, Function<Client, T> function) {
        return withClient(restConfig, function, getMetricName());
    }

    private static <T> T withClient(RestConfig restConfig, Function<Client, T> function, String metricName) {
        Client client = createClient(restConfig, metricName);
        try {
            return function.apply(client);
        } finally {
            client.close();
        }
    }

    @SneakyThrows
    private static SSLContext riktigSSLContext() {
        return SSLContext.getDefault();
    }

    @Value
    @Wither
    @Builder
    public static class RestConfig {

        @Builder.Default
        public int connectTimeout = 5000;
        @Builder.Default
        public int readTimeout = 15000;

        public boolean disableMetrics;
        public boolean disableParameterLogging;

    }

    private static String getMetricName() {
        StackTraceElement element = Thread.currentThread().getStackTrace()[3];
        return String.format("rest.client.%s.%s", element.getClassName(), element.getMethodName());
    }

    private static class Filter implements ClientResponseFilter, ClientRequestFilter {

        private static final String NAME = Filter.class.getName();
        private static final String CSRF_TOKEN = "csrf-token";

        private final String metricName;
        private final RestConfig restConfig;
        private final boolean useMetrics;

        private Filter(String metricName, RestConfig restConfig) {
            this.metricName = metricName;
            this.restConfig = restConfig;
            this.useMetrics = !restConfig.disableMetrics;
        }

        @Override
        public void filter(ClientRequestContext clientRequestContext) throws IOException {
            LOG.info("{} {}", clientRequestContext.getMethod(), uriForLogging(clientRequestContext));

            MultivaluedMap<String, Object> requestHeaders = clientRequestContext.getHeaders();

            of(MDC.get(MDCConstants.MDC_CORRELATION_ID))
                    .ifPresent(correlationId -> requestHeaders.add(CORRELATION_ID_HEADER_NAME, correlationId));

            requestHeaders.add(CSRF_COOKIE_NAVN, CSRF_TOKEN);
            requestHeaders.add(COOKIE, new Cookie(CSRF_COOKIE_NAVN, CSRF_TOKEN));


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
            return restConfig.disableParameterLogging ? UriBuilder.fromUri(uri).replaceQuery("").build() : uri;
        }

    }
}
