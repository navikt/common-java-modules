package no.nav.common.aktorregisterklient;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AktorregisterKlientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private static final Supplier<String> emptyTokenSupplier = () -> "";

    private static final String FNR_1 = "260xxx55159";
    private static final String AKTOR_ID_1 = "103xxx1557327";

    private static final String FNR_2 = "080xxx07100";
    private static final String AKTOR_ID_2 = "103xxx3839212";

    @Test
    public void lagerKorrektUrlForFnrTilAktorId() {
        AktorregisterKlient klient = new AktorregisterKlient("", "", null);

        String requestUrl= klient.createRequestUrl("https://test.local/aktoerregister/api/v1", AktorregisterKlient.Identgruppe.AktoerId);

        assertEquals("https://test.local/aktoerregister/api/v1/identer?gjeldende=true&identgruppe=AktoerId", requestUrl);
    }

    @Test
    public void lagerKorrektUrlForAktorIdTilFnr() {
        AktorregisterKlient klient = new AktorregisterKlient("", "", null);

        String requestUrl= klient.createRequestUrl("https://test.local/aktoerregister/api/v1", AktorregisterKlient.Identgruppe.NorskIdent);

        assertEquals("https://test.local/aktoerregister/api/v1/identer?gjeldende=true&identgruppe=NorskIdent", requestUrl);
    }

    @Test
    public void skalHenteAktorIdForFnr() {
        String json = TestUtils.readTestResourceFile("aktorid-to-fnr-single.json");
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AktorregisterKlient klient = new AktorregisterKlient(baseUrl, "test", emptyTokenSupplier);

        Optional<String> kanskjeAktorId = klient.hentAktorId(FNR_1);

        assertTrue(kanskjeAktorId.isPresent());
        assertEquals(kanskjeAktorId.get(), AKTOR_ID_1);
    }

    @Test
    public void skalHenteFlereAktorIdForFnr() {
        String json = TestUtils.readTestResourceFile("aktorid-to-fnr-multiple.json");
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AktorregisterKlient klient = new AktorregisterKlient(baseUrl, "test", emptyTokenSupplier);

        List<IdentOppslag> identOppslag = klient.hentAktorId(Arrays.asList(FNR_1, FNR_2));

        assertEquals(identOppslag.size(), 2);
        identOppslag.forEach(oppslag -> assertTrue(oppslag.getIdentFraRegister().isPresent()));
    }


    @Test
    public void skalHenteFnrForAktorId() {
        String json = TestUtils.readTestResourceFile("fnr-to-aktorid-single.json");
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AktorregisterKlient klient = new AktorregisterKlient(baseUrl, "test", emptyTokenSupplier);

        Optional<String> kanskjeFnr = klient.hentFnr(AKTOR_ID_1);

        assertTrue(kanskjeFnr.isPresent());
        assertEquals(kanskjeFnr.get(), FNR_1);
    }

    @Test
    public void skalHenteFlereFnrForAktorIder() {
        String json = TestUtils.readTestResourceFile("fnr-to-aktorid-multiple.json");
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AktorregisterKlient klient = new AktorregisterKlient(baseUrl, "test", emptyTokenSupplier);

        List<IdentOppslag> identOppslag = klient.hentFnr(Arrays.asList(AKTOR_ID_1, AKTOR_ID_2));

        assertEquals(identOppslag.size(), 2);
        identOppslag.forEach(oppslag -> assertTrue(oppslag.getIdentFraRegister().isPresent()));
    }

    @Test
    public void skalLeggeTilHeaders() {
        String json = TestUtils.readTestResourceFile("aktorid-to-fnr-single.json");
        String baseUrl = "http://localhost:" + wireMockRule.port();
        String authToken = "489h03n7092njkdsdsad";
        String consumingApplication = "my-test-app";

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AktorregisterKlient klient = new AktorregisterKlient(baseUrl, consumingApplication, () -> authToken);

        klient.hentFnr(AKTOR_ID_1);

        verify(getRequestedFor(anyUrl())
                .withHeader("Authorization", new EqualToPattern("Bearer " + authToken))
                .withHeader("Nav-Call-Id", new AnythingPattern())
                .withHeader("Nav-Consumer-Id", new EqualToPattern(consumingApplication))
                .withHeader("Nav-Personidenter", new EqualToPattern(AKTOR_ID_1))
        );
    }

}
