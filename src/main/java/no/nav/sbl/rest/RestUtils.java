package no.nav.sbl.rest;

import lombok.SneakyThrows;
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

    public static ClientConfig createClientConfig() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new JsonProvider());
        clientConfig.register(new MetricsProvider());
        clientConfig.property(FOLLOW_REDIRECTS, false);
        clientConfig.property(CONNECT_TIMEOUT, 5000);
        clientConfig.property(READ_TIMEOUT, 15000);


        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(100);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(20);
        clientConfig.property(CONNECTION_MANAGER,poolingHttpClientConnectionManager);
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        return clientConfig;
    }

    @SneakyThrows
    public static Client createClient() {
        return new JerseyClientBuilder()
                .sslContext(SSLContext.getDefault())
                .withConfig(createClientConfig())
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
