package no.nav.common.job.leader_election;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.common.utils.EnvironmentUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

import static java.lang.String.format;

@Slf4j
public class LeaderElectionHttpClient implements LeaderElectionClient {

    private final static Logger LOGGER = LoggerFactory.getLogger(LeaderElectionHttpClient.class);

    private final OkHttpClient client;

    public LeaderElectionHttpClient() {
        this.client = new OkHttpClient();
    }

    @SneakyThrows
    @Override
    public boolean isLeader() {
        String electorPath = EnvironmentUtils.getRequiredProperty("ELECTOR_PATH");

        Request request = new Request.Builder()
                .url("http://" + electorPath)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOGGER.error(format("Received unexpected status from leader election sidecar %d:%s", response.code(), response.message()));
                return false;
            }

            LeaderResponse leader = JsonUtils.fromJson(response.body().string(), LeaderResponse.class);

            return InetAddress.getLocalHost().getHostName().equals(leader.getName());
        } catch (Exception e) {
            LOGGER.error("Failed to check if pod is leader", e);
            return false;
        }
    }

}
