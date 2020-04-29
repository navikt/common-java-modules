package no.nav.common.leaderelection;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.test.junit.SystemPropertiesRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LeaderElectionHttpClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Rule
    public SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    private String hostName;

    @Before
    public void setUp() throws UnknownHostException {
        systemPropertiesRule.setProperty("ELECTOR_PATH", "localhost:" + wireMockRule.port());
        hostName = InetAddress.getLocalHost().getHostName();
    }

    @Test
    public void isLeaderIsTrue() {
        isLeader(true);

        LeaderElectionHttpClient leaderElectionHttpClient = new LeaderElectionHttpClient();

        assertTrue(leaderElectionHttpClient.isLeader());
    }

    @Test
    public void isLeaderIsFalse() {
        isLeader(false);

        LeaderElectionHttpClient leaderElectionHttpClient = new LeaderElectionHttpClient();

        assertFalse(leaderElectionHttpClient.isLeader());
    }

    private void isLeader(boolean value) {
        String name = value ? hostName : "not leader";

        givenThat(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"name\": \"" + name + "\"}"))
        );
    }
}
