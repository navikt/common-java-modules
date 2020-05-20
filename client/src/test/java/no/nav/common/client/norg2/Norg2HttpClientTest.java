package no.nav.common.client.norg2;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.client.TestUtils;
import no.nav.common.rest.client.RestUtils;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Norg2HttpClientTest {

    private static final String TEST_RESOURCE_BASE_PATH = "no/nav/common/client/norg2/";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void hentEnhet__skal_hente_enhet() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "enhet.json");
        Enhet jsonEnhet = RestUtils.parseJson(json, Enhet.class);
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get("/api/v1/enhet/1234")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        NorgHttp2Client client = new NorgHttp2Client(baseUrl);

        assertEquals(client.hentEnhet("1234"), jsonEnhet);
    }

    @Test
    public void alleAktiveEnheter__skal_hente_alle_enheter() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "enheter.json");
        Enhet[] jsonEnheter = RestUtils.parseJson(json, Enhet[].class);
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get("/api/v1/enhet?enhetStatusListe=AKTIV")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        NorgHttp2Client client = new NorgHttp2Client(baseUrl);
        List<Enhet> alleEnheter = client.alleAktiveEnheter();

        for (Enhet enhet : jsonEnheter) {
            assertTrue(alleEnheter.contains(enhet));
        }
    }

    @Test
    public void hentTilhorendeEnhet__skal_hente_enhet() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "enhet.json");
        Enhet jsonEnhet = RestUtils.parseJson(json, Enhet.class);
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get("/api/v1/enhet/navkontor/030105")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        NorgHttp2Client client = new NorgHttp2Client(baseUrl);

        assertEquals(client.hentTilhorendeEnhet("030105"), jsonEnhet);
    }

    @Test
    public void skal_pinge_riktig_url() {
        String baseUrl = "http://localhost:" + wireMockRule.port();
        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        Norg2Client client = new NorgHttp2Client(baseUrl);

        assertTrue(client.checkHealth().isHealthy());
        verify(getRequestedFor(urlEqualTo("/internal/isAlive")));
    }

}
