package no.nav.common.client.aktorregister;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import no.nav.common.client.TestUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AktorregisterHttpClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private static final Supplier<String> emptyTokenSupplier = () -> "";

    private static final Fnr FNR_1 = Fnr.of("260xxx55159");
    private static final AktorId AKTOR_ID_1 = AktorId.of("103xxx1557327");

    private static final Fnr FNR_2 = Fnr.of("080xxx07100");
    private static final AktorId AKTOR_ID_2 = AktorId.of("103xxx3839212");

    private static final String TEST_RESOURCE_BASE_PATH = "no/nav/common/client/aktorregister/";

    @Test
    public void skalHenteAktorIdForFnr() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "aktorid-to-fnr-single.json");
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AktorregisterHttpClient klient = new AktorregisterHttpClient(baseUrl, "test", emptyTokenSupplier);

        assertEquals(klient.hentAktorId(FNR_1), AKTOR_ID_1);
    }

    @Test
    public void skalHenteFlereAktorIdForFnr() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "aktorid-to-fnr-multiple.json");
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AktorregisterHttpClient klient = new AktorregisterHttpClient(baseUrl, "test", emptyTokenSupplier);

        List<IdentOppslag> identOppslag = klient.hentAktorId(Arrays.asList(FNR_1, FNR_2));

        assertEquals(identOppslag.size(), 2);
        identOppslag.forEach(oppslag -> assertTrue(oppslag.getIdentFraRegister().isPresent()));
    }


    @Test
    public void skalHenteFnrForAktorId() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "fnr-to-aktorid-single.json");
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AktorregisterHttpClient klient = new AktorregisterHttpClient(baseUrl, "test", emptyTokenSupplier);

        assertEquals(klient.hentFnr(AKTOR_ID_1), FNR_1);
    }

    @Test
    public void skalHenteFlereFnrForAktorIder() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "fnr-to-aktorid-multiple.json");
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AktorregisterHttpClient klient = new AktorregisterHttpClient(baseUrl, "test", emptyTokenSupplier);

        List<IdentOppslag> identOppslag = klient.hentFnr(Arrays.asList(AKTOR_ID_1, AKTOR_ID_2));

        assertEquals(identOppslag.size(), 2);
        identOppslag.forEach(oppslag -> assertTrue(oppslag.getIdentFraRegister().isPresent()));
    }

    @Test
    public void skalLeggeTilHeaders() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "aktorid-to-fnr-single.json");
        String baseUrl = "http://localhost:" + wireMockRule.port();
        String authToken = "489h03n7092njkdsdsad";
        String consumingApplication = "my-test-app";

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AktorregisterHttpClient klient = new AktorregisterHttpClient(baseUrl, consumingApplication, () -> authToken);

        klient.hentFnr(AKTOR_ID_1);

        verify(getRequestedFor(anyUrl())
                .withHeader("Authorization", new EqualToPattern("Bearer " + authToken))
                .withHeader("Nav-Call-Id", new AnythingPattern())
                .withHeader("Nav-Consumer-Id", new EqualToPattern(consumingApplication))
                .withHeader("Nav-Personidenter", new EqualToPattern(AKTOR_ID_1.get()))
        );
    }

    @Test
    public void skalBrukeKorrektUrlForAktorId() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "fnr-to-aktorid-single.json");
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AktorregisterHttpClient klient = new AktorregisterHttpClient(baseUrl, "", emptyTokenSupplier);

        klient.hentAktorId(FNR_1);

        verify(getRequestedFor(urlPathEqualTo("/identer"))
                .withQueryParam("gjeldende", new EqualToPattern("true"))
                .withQueryParam("identgruppe", new EqualToPattern("AktoerId"))
        );
    }

    @Test
    public void skalBrukeKorrektUrlForFnr() {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "aktorid-to-fnr-single.json");
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AktorregisterHttpClient klient = new AktorregisterHttpClient(baseUrl, "", emptyTokenSupplier);

        klient.hentFnr(AKTOR_ID_1);

        verify(getRequestedFor(urlPathEqualTo("/identer"))
                .withQueryParam("gjeldende", new EqualToPattern("true"))
                .withQueryParam("identgruppe", new EqualToPattern("NorskIdent"))
        );
    }

    @Test
    public void skal_pinge_riktig_url() {
        String baseUrl = "http://localhost:" + wireMockRule.port();
        givenThat(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        AktorregisterHttpClient client = new AktorregisterHttpClient(baseUrl, "", emptyTokenSupplier);

        assertTrue(client.checkHealth().isHealthy());
        verify(getRequestedFor(urlEqualTo("/internal/isAlive")));
    }

}
