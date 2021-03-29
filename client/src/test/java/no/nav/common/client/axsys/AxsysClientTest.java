package no.nav.common.client.axsys;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.client.TestUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class AxsysClientTest {

    private static final String TEST_RESOURCE_BASE_PATH = "no/nav/common/client/axsys/";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void hentAnsatte__skal_hente_ansatte() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "ansatte.json");
        List<AxsysEnhetBruker> jsonEnhet = JsonUtils.fromJsonArray(json, AxsysEnhetBruker.class);
        List<NavIdent> brukere = jsonEnhet.stream().map(AxsysEnhetBruker::getAppIdent).collect(Collectors.toList());
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get("/api/v1/enhet/1234/brukere")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AxsysClient client = new AxsysClientImpl(baseUrl);
        System.out.println(client.hentAnsatte(new EnhetId("1234")));
        assertEquals(client.hentAnsatte(new EnhetId("1234")), brukere);
    }


    @Test
    public void hentTilganger__skal_hente_tilganger() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "tilganger.json");
        AxsysEnheter jsonTilganger = JsonUtils.fromJson(json, AxsysEnheter.class);
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get("/api/v1/tilgang/Z123456")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AxsysClient client = new AxsysClientImpl(baseUrl);

        assertEquals(client.hentTilganger(new NavIdent("Z123456")), jsonTilganger);
    }
}
