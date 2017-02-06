package no.nav.sbl.dialogarena.common.abac;

import no.nav.sbl.dialogarena.common.abac.pep.*;
import no.nav.sbl.dialogarena.common.abac.pep.xacml.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.xacml.Utils;
import org.apache.http.entity.StringEntity;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static no.nav.sbl.dialogarena.common.abac.TestUtils.getContentFromJsonFile;
import static no.nav.sbl.dialogarena.common.abac.TestUtils.prepareResponse;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;


public class XacmlMapperTest {

    @Test
    public void convertsRequestToJson() throws IOException {

        final StringEntity stringEntity = XacmlMapper.mapRequestToEntity(MockXacmlRequest.getXacmlRequest());

        assertThat(stringEntity.getContentLength(), greaterThan(0L));

        String expectedContent = getContentFromJsonFile("xacmlrequest-withtoken.json");
        assertThat(Utils.entityToString(stringEntity), is(expectedContent));
    }

    @Test
    public void convertsSimpleJsonToResponse() throws IOException {
        final XacmlResponse actualResponse = XacmlMapper
                .mapRawResponse(prepareResponse(200, getContentFromJsonFile("xacmlresponse-simple.json")));

        XacmlResponse expectedResponse = getXacmlResponse();

        assertThat(actualResponse, is(equalTo(expectedResponse)));
    }

    @Test
    public void convertsSimpleJsonWithArrayToResponse() throws IOException {
        final XacmlResponse actualResponse = XacmlMapper
                .mapRawResponse(prepareResponse(200, getContentFromJsonFile("xacmlresponse-simple-with-array.json")));
        XacmlResponse expectedResponse = getXacmlResponse();

        assertThat(actualResponse, is(equalTo(expectedResponse)));
    }

    private XacmlResponse getXacmlResponse() {
        List<Response> responses = new ArrayList<>();
        responses.add(new Response().withDecision(Decision.Permit));
        return new XacmlResponse().withResponse(responses);
    }
}