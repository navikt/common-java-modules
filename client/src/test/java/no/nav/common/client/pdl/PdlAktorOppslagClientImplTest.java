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

public class PdlAktorOppslagClientImplTest {

    private static final String TEST_RESOURCE_BASE_PATH = "no/nav/common/client/pdl/aktor_oppslag/";

    @Test
    public void hentAktorId__skal_lage_riktig_request_og_hente_aktorid() {
        String graphqlJsonRequest = TestUtils.readTestResourceFileWithoutWhitespace(TEST_RESOURCE_BASE_PATH + "hent-aktorid-request.json");
        String graphqlJsonResponse = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "hent-aktorid-response.json");

        PdlClient pdlClient = mock(PdlClient.class);
        PdlAktorOppslagClientImpl pdlAktorOppslagClient = new PdlAktorOppslagClientImpl(pdlClient);
        ArgumentCaptor<GraphqlRequest> requestCaptor = ArgumentCaptor.forClass(GraphqlRequest.class);

        doReturn(JsonUtils.fromJson(graphqlJsonResponse, PdlAktorOppslagClientImpl.HentIdenterResponse.class))
                .when(pdlClient).request(requestCaptor.capture(), eq(PdlAktorOppslagClientImpl.HentIdenterResponse.class));

        AktorId aktorId = pdlAktorOppslagClient.hentAktorId(Fnr.of("1324"));

        assertEquals(graphqlJsonRequest, TestUtils.removeWhitespace(JsonUtils.toJson(requestCaptor.getValue())));
        assertEquals("111222333", aktorId.get());
    }

}
