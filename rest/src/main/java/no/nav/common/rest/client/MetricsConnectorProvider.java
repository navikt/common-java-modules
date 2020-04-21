package no.nav.common.rest.client;

import no.nav.common.rest.ClientLogFilter;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;
import java.util.concurrent.Future;

public class MetricsConnectorProvider implements ConnectorProvider {

    private final ClientLogFilter clientLogFilter;
    private final ConnectorProvider connectorProvider;

    public MetricsConnectorProvider(ConnectorProvider connectorProvider, ClientLogFilter clientLogFilter) {
        this.connectorProvider = connectorProvider;
        this.clientLogFilter = clientLogFilter;
    }

    @Override
    public Connector getConnector(Client client, Configuration runtimeConfig) {
        return new MetricsConnector(connectorProvider.getConnector(client, runtimeConfig));
    }

    private class MetricsConnector implements Connector {
        private final Connector connector;

        public MetricsConnector(Connector connector) {
            this.connector = connector;
        }

        @Override
        public ClientResponse apply(ClientRequest request) {
            try {
                return connector.apply(request);
            } catch (Throwable t) {
                clientLogFilter.requestFailed(request, t);
                throw t;
            }
        }

        @Override
        public Future<?> apply(ClientRequest request, AsyncConnectorCallback callback) {
            return connector.apply(request, callback);
        }

        @Override
        public String getName() {
            return connector.getName();
        }

        @Override
        public void close() {
            connector.close();
        }
    }
}
