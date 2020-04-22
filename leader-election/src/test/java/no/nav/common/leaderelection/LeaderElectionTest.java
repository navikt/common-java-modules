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

public class LeaderElectionTest {

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
    public void isLeaderIsTrue() throws UnknownHostException {

        isLeader(true);

        assertTrue(LeaderElection.isLeader());
    }

    @Test
    public void isLeaderIsFalse() throws UnknownHostException {

        isLeader(false);

        assertFalse(LeaderElection.isLeader());
    }

    private void isLeader(boolean value) throws UnknownHostException {
        String name = value ? hostName : "not leader";

        givenThat(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"name\": \"" + name + "\"}"))
        );
    }
}
