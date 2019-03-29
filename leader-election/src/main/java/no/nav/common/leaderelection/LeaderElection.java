package no.nav.common.leaderelection;

import org.json.JSONObject;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static java.lang.System.getProperty;
import static javax.ws.rs.client.ClientBuilder.newClient;

public class LeaderElection {
    private static Client client = newClient();

    public static boolean isLeader() {
        JSONObject leaderJson = getJSONFromUrl(getProperty("ELECTOR_PATH"));
        String leader = leaderJson.getString("name");
        try {
            return leader.equals(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private static JSONObject getJSONFromUrl(String url) {
        return client.target("http://" + url)
                .request(MediaType.APPLICATION_JSON)
                .get(JSONObject.class);
    }
}
