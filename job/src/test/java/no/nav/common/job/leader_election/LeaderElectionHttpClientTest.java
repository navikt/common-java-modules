package no.nav.common.job.leader_election;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static no.nav.common.test.wiremock.WireMockTestUtils.createWireMockExtension;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.InetAddress;
import java.net.UnknownHostException;
import no.nav.common.test.junit.SystemPropertiesRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class LeaderElectionHttpClientTest {

    @RegisterExtension
    public WireMockExtension wireMockRule = createWireMockExtension();

    @RegisterExtension
    public SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    private String hostName;

    @BeforeEach
    public void setUp() throws UnknownHostException {
        systemPropertiesRule.setProperty("ELECTOR_PATH", "localhost:" + wireMockRule.getPort());
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

        wireMockRule.stubFor(
                get(urlEqualTo("/")).willReturn(aResponse().withStatus(200).withBody("{\"name\": \"" + name + "\"}")));
    }
}
