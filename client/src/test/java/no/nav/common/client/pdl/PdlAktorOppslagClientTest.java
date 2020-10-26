package no.nav.common.client.pdl;

import no.nav.common.client.TestUtils;
import no.nav.common.client.utils.graphql.GraphqlRequest;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
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

}
