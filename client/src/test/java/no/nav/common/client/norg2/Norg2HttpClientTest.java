package no.nav.common.client.norg2;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static no.nav.common.test.wiremock.WireMockTestUtils.createWireMockExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import no.nav.common.client.TestUtils;
import no.nav.common.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class Norg2HttpClientTest {
    private static final String TEST_RESOURCE_BASE_PATH = "no/nav/common/client/norg2/";

    @RegisterExtension
    public WireMockExtension wireMockRule = createWireMockExtension();

    @Test
    public void hentEnhet__skal_hente_enhet() throws Exception {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "enhet.json");
        Enhet jsonEnhet = JsonUtils.fromJson(json, Enhet.class);
        String baseUrl = "http://localhost:" + wireMockRule.getPort();

        wireMockRule.stubFor(
                get("/api/v1/enhet/1234").willReturn(aResponse().withStatus(200).withBody(json)));

        NorgHttp2Client client = new NorgHttp2Client(baseUrl);

        assertEquals(jsonEnhet, client.hentEnhet("1234"));
    }

    @Test
    public void alleAktiveEnheter__skal_hente_alle_enheter() throws Exception {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "enheter.json");
        List<Enhet> jsonEnheter = JsonUtils.fromJsonArray(json, Enhet.class);
        String baseUrl = "http://localhost:" + wireMockRule.getPort();

        wireMockRule.stubFor(get("/api/v1/enhet?enhetStatusListe=AKTIV")
                .willReturn(aResponse().withStatus(200).withBody(json)));

        NorgHttp2Client client = new NorgHttp2Client(baseUrl);
        List<Enhet> alleEnheter = client.alleAktiveEnheter();

        for (Enhet enhet : jsonEnheter) {
            assertTrue(alleEnheter.contains(enhet));
        }
    }

    @Test
    public void hentTilhorendeEnhet__skal_hente_enhet_uten_disk_og_skjermet() throws Exception {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "enhet.json");
        Enhet jsonEnhet = JsonUtils.fromJson(json, Enhet.class);
        String baseUrl = "http://localhost:" + wireMockRule.getPort();

        wireMockRule.stubFor(get("/api/v1/enhet/navkontor/030105")
                .willReturn(aResponse().withStatus(200).withBody(json)));

        NorgHttp2Client client = new NorgHttp2Client(baseUrl);

        assertEquals(jsonEnhet, client.hentTilhorendeEnhet("030105", null, false));
    }

    @Test
    public void hentTilhorendeEnhet__skal_hente_enhet_med_disk_og_skjermet() throws Exception {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "enhet.json");
        Enhet jsonEnhet = JsonUtils.fromJson(json, Enhet.class);
        String baseUrl = "http://localhost:" + wireMockRule.getPort();

        wireMockRule.stubFor(get("/api/v1/enhet/navkontor/030105?disk=SPFO&skjermet=true")
                .willReturn(aResponse().withStatus(200).withBody(json)));

        NorgHttp2Client client = new NorgHttp2Client(baseUrl);

        assertEquals(jsonEnhet, client.hentTilhorendeEnhet("030105", Norg2Client.Diskresjonskode.SPFO, true));
    }

    @Test
    public void skal_pinge_riktig_url() {
        String baseUrl = "http://localhost:" + wireMockRule.getPort();
        wireMockRule.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        Norg2Client client = new NorgHttp2Client(baseUrl);

        assertTrue(client.checkHealth().isHealthy());
        wireMockRule.verify(getRequestedFor(urlEqualTo("/internal/health/readiness")));
    }
}
