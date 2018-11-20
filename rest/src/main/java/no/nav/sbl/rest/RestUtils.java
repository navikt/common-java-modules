package no.nav.sbl.rest;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.Wither;
import no.nav.json.JsonProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import java.util.function.Function;

import static org.glassfish.jersey.client.ClientProperties.*;

public class RestUtils {

    public static final String CSRF_COOKIE_NAVN = "NAV_CSRF_PROTECTION";

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
        ClientLogFilter clientLogFilter = new ClientLogFilter(ClientLogFilter.ClientLogFilterConfig.builder()
                .disableMetrics(restConfig.disableMetrics)
                .disableParameterLogging(restConfig.disableParameterLogging)
                .metricName(metricName)
                .build());
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new JsonProvider());
        clientConfig.register(clientLogFilter);
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

}
