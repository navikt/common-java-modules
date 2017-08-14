package no.nav.fo.feed.util;


import no.nav.json.JsonProvider;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class RestUtils {

    public static Client getClient() {
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        clientBuilder.register(new JsonProvider());
        return clientBuilder.build();
    }

}
