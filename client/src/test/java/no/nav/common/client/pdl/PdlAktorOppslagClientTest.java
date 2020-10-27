package no.nav.common.client.pdl;

import no.nav.common.client.TestUtils;
import no.nav.common.client.utils.graphql.GraphqlRequest;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class PdlAktorOppslagClientTest {

    private static final String TEST_RESOURCE_BASE_PATH = "no/nav/common/client/pdl/aktor_oppslag/";

    @Test
    public void hentAktorId__skal_lage_riktig_request_og_hente_aktorid() {
        String graphqlJsonRequest = TestUtils.readTestResourceFileWithoutWhitespace(TEST_RESOURCE_BASE_PATH + "hent-aktorid-request.json");
        String graphqlJsonResponse = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "hent-aktorid-response.json");

        PdlClient pdlClient = mock(PdlClient.class);
        PdlAktorOppslagClient pdlAktorOppslagClient = new PdlAktorOppslagClient(pdlClient);
        ArgumentCaptor<GraphqlRequest> requestCaptor = ArgumentCaptor.forClass(GraphqlRequest.class);

        doReturn(JsonUtils.fromJson(graphqlJsonResponse, PdlAktorOppslagClient.HentIdenterResponse.class))
                .when(pdlClient).request(requestCaptor.capture(), eq(PdlAktorOppslagClient.HentIdenterResponse.class));

        AktorId aktorId = pdlAktorOppslagClient.hentAktorId(Fnr.of("1234567890"));

        assertEquals(graphqlJsonRequest, TestUtils.removeWhitespace(JsonUtils.toJson(requestCaptor.getValue())));
        assertEquals("111222333", aktorId.get());
    }

    @Test
    public void hentFnr__skal_lage_riktig_request_og_hente_fnr() {
        String graphqlJsonRequest = TestUtils.readTestResourceFileWithoutWhitespace(TEST_RESOURCE_BASE_PATH + "hent-fnr-request.json");
        String graphqlJsonResponse = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "hent-fnr-response.json");

        PdlClient pdlClient = mock(PdlClient.class);
        PdlAktorOppslagClient pdlAktorOppslagClient = new PdlAktorOppslagClient(pdlClient);
        ArgumentCaptor<GraphqlRequest> requestCaptor = ArgumentCaptor.forClass(GraphqlRequest.class);

        doReturn(JsonUtils.fromJson(graphqlJsonResponse, PdlAktorOppslagClient.HentIdenterResponse.class))
                .when(pdlClient).request(requestCaptor.capture(), eq(PdlAktorOppslagClient.HentIdenterResponse.class));

        Fnr fnr = pdlAktorOppslagClient.hentFnr(AktorId.of("111222333"));

        assertEquals(graphqlJsonRequest, TestUtils.removeWhitespace(JsonUtils.toJson(requestCaptor.getValue())));
        assertEquals("1234567890", fnr.get());
    }

    @Test
    public void hentAktorIdBolk__skal_lage_riktig_request_og_hente_aktorid_bolk() {
        String graphqlJsonRequest = TestUtils.readTestResourceFileWithoutWhitespace(TEST_RESOURCE_BASE_PATH + "hent-aktorid-bolk-request.json");
        String graphqlJsonResponse = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "hent-ident-bolk-response.json");

        PdlClient pdlClient = mock(PdlClient.class);
        PdlAktorOppslagClient pdlAktorOppslagClient = new PdlAktorOppslagClient(pdlClient);
        ArgumentCaptor<GraphqlRequest> requestCaptor = ArgumentCaptor.forClass(GraphqlRequest.class);

        doReturn(JsonUtils.fromJson(graphqlJsonResponse, PdlAktorOppslagClient.HentIdenterBolkResponse.class))
                .when(pdlClient).request(requestCaptor.capture(), eq(PdlAktorOppslagClient.HentIdenterBolkResponse.class));

        Fnr fnr1 = Fnr.of("1234567890");
        Fnr fnr2 = Fnr.of("4444444444");
        Fnr fnr3 = Fnr.of("0987654321");

        Map<Fnr, AktorId> identMapping = pdlAktorOppslagClient.hentAktorIdBolk(List.of(fnr1, fnr2, fnr3));

        assertEquals(graphqlJsonRequest, TestUtils.removeWhitespace(JsonUtils.toJson(requestCaptor.getValue())));

        assertEquals(2, identMapping.size());
        assertEquals(AktorId.of("1122334455"), identMapping.get(fnr1));
        assertFalse(identMapping.containsKey(fnr2));
        assertEquals(AktorId.of("22224444"), identMapping.get(fnr3));
    }

    @Test
    public void hentFnrBolk__skal_lage_riktig_request_og_hente_fnr_bolk() {
        String graphqlJsonRequest = TestUtils.readTestResourceFileWithoutWhitespace(TEST_RESOURCE_BASE_PATH + "hent-fnr-bolk-request.json");
        String graphqlJsonResponse = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "hent-ident-bolk-response.json");

        PdlClient pdlClient = mock(PdlClient.class);
        PdlAktorOppslagClient pdlAktorOppslagClient = new PdlAktorOppslagClient(pdlClient);
        ArgumentCaptor<GraphqlRequest> requestCaptor = ArgumentCaptor.forClass(GraphqlRequest.class);

        doReturn(JsonUtils.fromJson(graphqlJsonResponse, PdlAktorOppslagClient.HentIdenterBolkResponse.class))
                .when(pdlClient).request(requestCaptor.capture(), eq(PdlAktorOppslagClient.HentIdenterBolkResponse.class));

        AktorId aktorId1 = AktorId.of("1122334455");
        AktorId aktorId2 = AktorId.of("555555555");
        AktorId aktorId3 = AktorId.of("22224444");

        Map<AktorId, Fnr> identMapping = pdlAktorOppslagClient.hentFnrBolk(List.of(aktorId1, aktorId2, aktorId3));

        assertEquals(graphqlJsonRequest, TestUtils.removeWhitespace(JsonUtils.toJson(requestCaptor.getValue())));

        assertEquals(2, identMapping.size());
        assertEquals(Fnr.of("1234567890"), identMapping.get(aktorId1));
        assertFalse(identMapping.containsKey(aktorId2));
        assertEquals(Fnr.of("0987654321"), identMapping.get(aktorId3));
    }

}
