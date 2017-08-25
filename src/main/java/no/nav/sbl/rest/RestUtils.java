package no.nav.sbl.rest;

import lombok.SneakyThrows;
import no.nav.json.JsonProvider;
import org.glassfish.jersey.client.ClientConfig;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import java.util.function.Function;

import static org.glassfish.jersey.client.ClientProperties.CONNECT_TIMEOUT;
import static org.glassfish.jersey.client.ClientProperties.FOLLOW_REDIRECTS;
import static org.glassfish.jersey.client.ClientProperties.READ_TIMEOUT;

public class RestUtils {

    @SneakyThrows
    public static Client createClient() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new JsonProvider());
        clientConfig.property(FOLLOW_REDIRECTS, false);
        clientConfig.property(CONNECT_TIMEOUT, 5000);
        clientConfig.property(READ_TIMEOUT, 15000);
        return ClientBuilder.newBuilder()
                .sslContext(SSLContext.getDefault())
                .withConfig(clientConfig)
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

}
