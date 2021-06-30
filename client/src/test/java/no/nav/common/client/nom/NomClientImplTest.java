package no.nav.common.client.nom;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.client.TestUtils;
import no.nav.common.types.identer.NavIdent;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static org.junit.Assert.assertEquals;

public class NomClientImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private static final String TEST_RESOURCE_BASE_PATH = "no/nav/common/client/nom/";

    @Test
    public void skal_lage_riktig_request_og_parse_response() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        String graphqlJsonRequest = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "ressurser-request.json");
        String graphqlJsonResponse = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "ressurser-response.json");

        VeilederVisningsnavn veilederVisningsnavn1 = new VeilederVisningsnavn()
                .setNavIdent(NavIdent.of("Z1234"))
                .setFornavn("F1234 M1234")
                .setEtternavn("E1234")
                .setVisningsNavn("E1234, F1234 M1234");

        VeilederVisningsnavn veilederVisningsnavn2 = new VeilederVisningsnavn()
                .setNavIdent(NavIdent.of("Z5678"))
                .setFornavn("F5678")
                .setEtternavn("E5678")
                .setVisningsNavn("E5678, F5678");

        NavIdent identTilVeilederSomIkkeFinnes = NavIdent.of("Z7777");

        NomClientImpl nomClient = new NomClientImpl(apiUrl, () -> "SERVICE_TOKEN");

        givenThat(post(urlEqualTo("/graphql"))
                .withHeader(ACCEPT, equalTo(MEDIA_TYPE_JSON.toString()))
                .withHeader("Authorization", equalTo("Bearer SERVICE_TOKEN"))
                .withRequestBody(equalToJson(graphqlJsonRequest))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(graphqlJsonResponse))
        );

        List<VeilederVisningsnavn> veilederVisningsnavnList = nomClient.finnVisningsnavn(
                List.of(
                        veilederVisningsnavn1.navIdent,
                        veilederVisningsnavn2.navIdent,
                        identTilVeilederSomIkkeFinnes
                )
        );

        assertEquals(2, veilederVisningsnavnList.size());
        assertEquals(veilederVisningsnavn1, veilederVisningsnavnList.get(0));
        assertEquals(veilederVisningsnavn2, veilederVisningsnavnList.get(1));
    }

    @Test
    public void skal_batche_requests_mot_nom() {
        String apiUrl = "http://localhost:" + wireMockRule.port();
        String graphqlJsonResponse = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "ressurser-response.json");

        NomClientImpl nomClient = new NomClientImpl(apiUrl, () -> "SERVICE_TOKEN");

        givenThat(post(urlEqualTo("/graphql"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(graphqlJsonResponse))
        );

        List<NavIdent> navIdenter = new ArrayList<>();
        for (int i = 0; i < 420; i++) {
            navIdenter.add(NavIdent.of("Z231231"));
        }

        nomClient.finnVisningsnavn(navIdenter);

        verify(exactly(5), postRequestedFor(urlEqualTo("/graphql")));
    }


}
