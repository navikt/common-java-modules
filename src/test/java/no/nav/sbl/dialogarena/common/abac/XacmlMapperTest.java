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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.IsEqual.equalTo;


public class XacmlMapperTest {

    private static final String ID1 = "no.nav.abac.advice.ingen_tilgang";
    private static final String ID2 = "no.nav.abac.advices.deny.reason";

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

    @Test
    public void convertsJsonWithAdvicesToResponse() throws IOException {
        final XacmlResponse actualResponse = XacmlMapper
                .mapRawResponse(prepareResponse(200, getContentFromJsonFile("xacmlresponse_multiple_advice.json")));
        XacmlResponse expectedResponse = getXacmlResponseWithAdvices();

        assertThat(actualResponse, is(equalTo(expectedResponse)));
    }

    private XacmlResponse getXacmlResponse() {
        List<Response> responses = new ArrayList<>();
        responses.add(new Response().withDecision(Decision.Permit));
        return new XacmlResponse().withResponse(responses);
    }

    private XacmlResponse getXacmlResponseWithAdvices() {
        List<Advice> associatedAdvice = new ArrayList<>();

        final AttributeAssignment attributeAssignment1 = new AttributeAssignment("no.nav.abac.advice.fritekst", "Mangler konsument (consumerId)");
        final Advice advice1 = new Advice(ID1, attributeAssignment1);
        associatedAdvice.add(advice1);

        final AttributeAssignment attributeAssignment2 = new AttributeAssignment("no.nav.abac.advice.fritekst", "Mangler autentiseringsNivaa (authenticationLevel)");
        final Advice advice2 = new Advice(ID2, attributeAssignment2);
        associatedAdvice.add(advice2);

        List<Response> responses = new ArrayList<>();
        responses.add(new Response().withDecision(Decision.Deny).withAssociatedAdvice(associatedAdvice));

        return new XacmlResponse().withResponse(responses);
    }

}