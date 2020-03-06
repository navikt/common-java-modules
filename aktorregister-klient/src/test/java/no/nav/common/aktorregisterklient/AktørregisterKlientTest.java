package no.nav.common.aktorregisterklient;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AktørregisterKlientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private static final Supplier<String> emptyTokenSupplier = () -> "";

    private static final String FNR_1 = "260xxx55159";
    private static final String AKTØR_ID_1 = "103xxx1557327";

    private static final String FNR_2 = "080xxx07100";
    private static final String AKTØR_ID_2 = "103xxx3839212";

    @Test
    public void lagerKorrektUrlForFnrTilAktørId() {
        AktørregisterKlient klient = new AktørregisterKlient("", "", null);

        String requestUrl= klient.createRequestUrl("https://app-q0.adeo.no/aktoerregister/api/v1", AktørregisterKlient.Identgruppe.AktoerId);

        assertEquals("https://app-q0.adeo.no/aktoerregister/api/v1/identer?gjeldende=true&identgruppe=AktoerId", requestUrl);
    }

    @Test
    public void lagerKorrektUrlForAktørIdTilFnr() {
        AktørregisterKlient klient = new AktørregisterKlient("", "", null);

        String requestUrl= klient.createRequestUrl("https://app-q0.adeo.no/aktoerregister/api/v1", AktørregisterKlient.Identgruppe.NorskIdent);

        assertEquals("https://app-q0.adeo.no/aktoerregister/api/v1/identer?gjeldende=true&identgruppe=NorskIdent", requestUrl);
    }

    @Test
    public void skalHenteAktørIdForFnr() {
        String json = TestUtils.readTestResourceFile("aktorid-to-fnr-single.json");
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AktørregisterKlient klient = new AktørregisterKlient(baseUrl, "test", emptyTokenSupplier);

        Optional<String> kanskjeAktørId = klient.hentAktørId(FNR_1);

        assertTrue(kanskjeAktørId.isPresent());
        assertEquals(kanskjeAktørId.get(), AKTØR_ID_1);
    }

    @Test
    public void skalHenteFlereAktørIdForFnr() {
        String json = TestUtils.readTestResourceFile("aktorid-to-fnr-multiple.json");
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AktørregisterKlient klient = new AktørregisterKlient(baseUrl, "test", emptyTokenSupplier);

        List<Map.Entry<String, Optional<String>>> identEntries = klient.hentFlereAktørIder(Arrays.asList(FNR_1, FNR_2));

        assertEquals(identEntries.size(), 2);
        identEntries.forEach(entry -> assertTrue(entry.getValue().isPresent()));
    }


    @Test
    public void skalHenteFnrForAktørId() {
        String json = TestUtils.readTestResourceFile("fnr-to-aktorid-single.json");
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AktørregisterKlient klient = new AktørregisterKlient(baseUrl, "test", emptyTokenSupplier);

        Optional<String> kanskjeFnr = klient.hentFnr(AKTØR_ID_1);

        assertTrue(kanskjeFnr.isPresent());
        assertEquals(kanskjeFnr.get(), FNR_1);
    }

    @Test
    public void skalHenteFlereFnrForAktørIder() {
        String json = TestUtils.readTestResourceFile("fnr-to-aktorid-multiple.json");
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AktørregisterKlient klient = new AktørregisterKlient(baseUrl, "test", emptyTokenSupplier);

        List<Map.Entry<String, Optional<String>>> identEntries = klient.hentFlereFnr(Arrays.asList(AKTØR_ID_1, AKTØR_ID_2));

        assertEquals(identEntries.size(), 2);
        identEntries.forEach(entry -> assertTrue(entry.getValue().isPresent()));
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

        AktørregisterKlient klient = new AktørregisterKlient(baseUrl, consumingApplication, () -> authToken);

        klient.hentFnr(AKTØR_ID_1);

        verify(getRequestedFor(anyUrl())
                .withHeader("Authorization", new EqualToPattern("Bearer " + authToken))
                .withHeader("Nav-Call-Id", new AnythingPattern())
                .withHeader("Nav-Consumer-Id", new EqualToPattern(consumingApplication))
                .withHeader("Nav-Personidenter", new EqualToPattern(AKTØR_ID_1))
        );
    }

}
