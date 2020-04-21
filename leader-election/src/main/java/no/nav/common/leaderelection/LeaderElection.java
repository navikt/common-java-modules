package no.nav.common.leaderelection;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.RestUtils;
import no.nav.common.utils.EnvironmentUtils;

import java.net.InetAddress;

@Slf4j
public class LeaderElection {

    @SneakyThrows
    public static boolean isLeader() {
        String electorPath = EnvironmentUtils.getRequiredProperty("ELECTOR_PATH");

        String entity = RestUtils.withClient(client -> client
                .target("http://" + electorPath)
                .request()
                .get()
                .readEntity(String.class)
        );

        LeaderResponse leader = JsonUtils.fromJson(entity, LeaderResponse.class);

        return InetAddress.getLocalHost().getHostName().equals(leader.getName());
    }

    public static boolean isNotLeader() {
        return !isLeader();
    }
}
