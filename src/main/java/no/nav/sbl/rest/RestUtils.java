package no.nav.sbl.rest;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import no.nav.json.JsonProvider;
import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.*;
import java.io.IOException;
import java.net.URI;
import java.util.function.Function;

import static org.glassfish.jersey.apache.connector.ApacheClientProperties.CONNECTION_MANAGER;
import static org.glassfish.jersey.client.ClientProperties.*;

public class RestUtils {

    public static final RestConfig DEFAULT_CONFIG = RestConfig.builder().build();

    @SuppressWarnings("unused")
    public static ClientConfig createClientConfig() {
        return createClientConfig(DEFAULT_CONFIG);
    }

    public static ClientConfig createClientConfig(RestConfig restConfig) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new JsonProvider());
        if (!restConfig.disableMetrics) {
            clientConfig.register(new MetricsProvider());
        }
        clientConfig.property(FOLLOW_REDIRECTS, false);
        clientConfig.property(CONNECT_TIMEOUT, restConfig.connectTimeout);
        clientConfig.property(READ_TIMEOUT, restConfig.readTimeout);

        if (!restConfig.disableConnectionPooling) {
            PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
            poolingHttpClientConnectionManager.setMaxTotal(100);
            poolingHttpClientConnectionManager.setDefaultMaxPerRoute(20);
            clientConfig.property(CONNECTION_MANAGER, poolingHttpClientConnectionManager);
            clientConfig.connectorProvider(new ApacheConnectorProvider());
        }
        return clientConfig;
    }

    @SneakyThrows
    public static Client createClient() {
        return createClient(DEFAULT_CONFIG);
    }

    @SneakyThrows
    public static Client createClient(RestConfig restConfig) {
        return new JerseyClientBuilder()
                .sslContext(SSLContext.getDefault())
                .withConfig(createClientConfig(restConfig))
                .build();
    }

    @SneakyThrows
    public static <T> T withClient(Function<Client, T> function) {
        Client client = createClient();
        try {
            return function.apply(client);
        } finally {
            client.close();
        }
    }

    @Value
    @Builder
    public static class RestConfig {

        @Builder.Default
        public int connectTimeout = 5000;
        @Builder.Default
        public int readTimeout = 15000;

        public boolean disableConnectionPooling;
        public boolean disableMetrics;
    }

    private static class MetricsProvider implements ClientResponseFilter, ClientRequestFilter {

        public static final String NAME = MetricsProvider.class.getName();

        @Override
        public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext) throws IOException {
            Timer timer = (Timer) clientRequestContext.getProperty(NAME);
            timer
                    .stop()
                    .addFieldToReport("httpStatus", clientResponseContext.getStatus())
                    .report();
        }

        @Override
        public void filter(ClientRequestContext clientRequestContext) throws IOException {
            Timer timer = MetricsFactory.createTimer(timerNavn(clientRequestContext.getUri()));
            timer.start();
            clientRequestContext.setProperty(NAME, timer);
        }

        private String timerNavn(URI uri) {
            return String.format("rest.client.%s%s",
                    uri.getHost(),
                    uri.getPath()
            );
        }

    }

}
