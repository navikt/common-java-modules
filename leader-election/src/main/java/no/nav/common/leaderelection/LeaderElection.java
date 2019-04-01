package no.nav.common.leaderelection;

import lombok.SneakyThrows;
import no.nav.json.JsonUtils;
import no.nav.sbl.rest.RestUtils;
import org.json.JSONObject;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static javax.ws.rs.client.ClientBuilder.newClient;

public class LeaderElection {

    @SneakyThrows
    public static boolean isLeader() {
        String electorPath = System.getenv("ELECTOR_PATH");
        if (electorPath == null) {
            throw new RuntimeException("Fant ikke ELECTOR_PATH, husk å sett `leaderElection: true` i nais.yaml");
        }

        String entity = RestUtils.withClient(client -> client
                .target("http://" + electorPath)
                .request()
                .get()
                .readEntity(String.class)
        );

        LeaderResponse leader = JsonUtils.fromJson(entity, LeaderResponse.class);

        return InetAddress.getLocalHost().getHostName().equals(leader.getName());
    }
}
