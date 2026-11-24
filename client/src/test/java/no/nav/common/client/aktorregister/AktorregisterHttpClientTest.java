package no.nav.common.client.aktorregister;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Arrays.asList;
import static no.nav.common.test.wiremock.WireMockTestUtils.createWireMockExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import no.nav.common.client.TestUtils;
import no.nav.common.client.aktoroppslag.AktorregisterHttpClient;
import no.nav.common.client.aktoroppslag.BrukerIdenter;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AktorregisterHttpClientTest {

    @RegisterExtension
    public WireMockExtension wireMockRule = createWireMockExtension();

    private static final String testApplication = "my-test-app";
    private static final String dummyAuthToken = "489h03n7092njkdsdsad";
    private static final Supplier<String> dummyTokenSupplier = () -> dummyAuthToken;

    private static final Fnr FNR_1 = Fnr.of("260xxx55159");
    private static final AktorId AKTOR_ID_1 = AktorId.of("103xxx1557327");

    private static final Fnr FNR_2 = Fnr.of("080xxx07100");
    private static final AktorId AKTOR_ID_2 = AktorId.of("103xxx3839212");

    private static final Fnr FNR_3 = Fnr.of("672xxx879432");
    private static final AktorId AKTOR_ID_3 = AktorId.of("893xxx4490350");

    private static final String TEST_RESOURCE_BASE_PATH = "no/nav/common/client/aktorregister/";

    AktorregisterHttpClient klient;

    @BeforeEach
    public void setup() {
        String baseUrl = "http://localhost:" + wireMockRule.getPort();
        klient = new AktorregisterHttpClient(baseUrl, testApplication, dummyTokenSupplier);
    }

    @Test
    public void hentAktorId__skal_hente_aktor_id_for_fnr() throws Exception {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "aktorid-to-fnr-single.json");

        wireMockRule.stubFor(
                get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(json)));

        assertEquals(AKTOR_ID_1, klient.hentAktorId(FNR_1));
    }

    @Test
    public void hentAktorIdBolk__skal_hente_flere_aktor_id_for_fnr() throws Exception {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "aktorid-to-fnr-multiple.json");

        wireMockRule.stubFor(
                get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(json)));

        Map<Fnr, AktorId> identOppslag = klient.hentAktorIdBolk(Arrays.asList(FNR_1, FNR_2, FNR_3));

        assertEquals(2, identOppslag.size());
        assertEquals(AKTOR_ID_1, identOppslag.get(FNR_1));
        assertEquals(AKTOR_ID_2, identOppslag.get(FNR_2));
        assertNull(identOppslag.get(FNR_3));
    }

    @Test
    public void hentFnr__skal_hente_fnr_for_aktor_id() throws Exception {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "fnr-to-aktorid-single.json");

        wireMockRule.stubFor(
                get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(json)));

        assertEquals(FNR_1, klient.hentFnr(AKTOR_ID_1));
    }

    @Test
    public void hentAktorIder__skal_hente_flere_fnr_for_aktor_id() throws Exception {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "fnr-to-multiple-aktorid.json");

        wireMockRule.stubFor(
                get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(json)));

        List<AktorId> identOppslag = klient.hentAktorIder(FNR_1);

        assertEquals(2, identOppslag.size());
        assertEquals(AKTOR_ID_1, identOppslag.get(0));
        assertEquals(AKTOR_ID_2, identOppslag.get(1));
    }

    @Test
    public void hentFnrBolk__skal_hente_flere_fnr_for_aktor_ider() throws Exception {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "fnr-to-aktorid-multiple.json");

        wireMockRule.stubFor(
                get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(json)));

        Map<AktorId, Fnr> identOppslag = klient.hentFnrBolk(Arrays.asList(AKTOR_ID_1, AKTOR_ID_2, AKTOR_ID_3));

        assertEquals(2, identOppslag.size());
        assertEquals(FNR_1, identOppslag.get(AKTOR_ID_1));
        assertEquals(FNR_2, identOppslag.get(AKTOR_ID_2));
        assertNull(identOppslag.get(AKTOR_ID_3));
    }

    @Test
    public void hentAktorId__skal_bruke_korrekt_url_for_aktor_id() throws Exception {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "fnr-to-aktorid-single.json");

        wireMockRule.stubFor(
                get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(json)));

        klient.hentAktorId(FNR_1);

        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/identer"))
                .withQueryParam("gjeldende", new EqualToPattern("true"))
                .withQueryParam("identgruppe", new EqualToPattern("AktoerId")));
    }

    @Test
    public void hentFnr__skal_bruke_korrekt_url_for_fnr() throws Exception {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "aktorid-to-fnr-single.json");

        wireMockRule.stubFor(
                get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(json)));

        klient.hentFnr(AKTOR_ID_1);

        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/identer"))
                .withQueryParam("gjeldende", new EqualToPattern("true"))
                .withQueryParam("identgruppe", new EqualToPattern("NorskIdent")));
    }

    @Test
    public void hentIdenter__skal_lage_riktig_request_og_hente_gjeldende_og_historiske_identer() throws Exception {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "hent-identer-med-historikk.json");
        Fnr fnr = Fnr.of("22222");

        wireMockRule.stubFor(get(urlEqualTo("/identer"))
                .withHeader("Authorization", new EqualToPattern("Bearer " + dummyAuthToken))
                .withHeader("Nav-Call-Id", new AnythingPattern())
                .withHeader("Nav-Consumer-Id", new EqualToPattern(testApplication))
                .withHeader("Nav-Personidenter", new EqualToPattern(fnr.get()))
                .willReturn(aResponse().withStatus(200).withBody(json)));

        BrukerIdenter brukerIdenter = klient.hentIdenter(fnr);

        BrukerIdenter forventet = new BrukerIdenter(
                Fnr.of("33333"),
                AktorId.of("55555"),
                asList(Fnr.of("22222"), Fnr.of("44444")),
                List.of(AktorId.of("11111")));

        assertEquals(forventet, brukerIdenter);
    }

    @Test
    public void skal_legge_til_headers() throws Exception {
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "aktorid-to-fnr-single.json");

        wireMockRule.stubFor(
                get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(json)));

        klient.hentFnr(AKTOR_ID_1);

        wireMockRule.verify(getRequestedFor(anyUrl())
                .withHeader("Authorization", new EqualToPattern("Bearer " + dummyAuthToken))
                .withHeader("Nav-Call-Id", new AnythingPattern())
                .withHeader("Nav-Consumer-Id", new EqualToPattern(testApplication))
                .withHeader("Nav-Personidenter", new EqualToPattern(AKTOR_ID_1.get())));
    }

    @Test
    public void skal_pinge_riktig_url() {
        wireMockRule.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)));

        assertTrue(klient.checkHealth().isHealthy());
        wireMockRule.verify(getRequestedFor(urlEqualTo("/internal/isAlive")));
    }
}
