package no.nav.sbl.dialogarena.common.abac.pep.service;

import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.sbl.dialogarena.common.abac.pep.MockXacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.*;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Response;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response.Status.Family;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.setProperty;
import static javax.ws.rs.core.Response.Status.Family.familyOf;
import static no.nav.sbl.dialogarena.common.abac.TestUtils.getContentFromJsonFile;
import static no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants.SYSTEMUSER_USERNAME;
import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacService.ABAC_ENDPOINT_URL_PROPERTY_NAME;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.UNAUTHORIZED_401;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbacServiceTest {

    @InjectMocks
    AbacService abacService;

    private static final String SYSTEMUSER_PASSWORD = "systemuserpassword";
    private static final String SYSTEMUSER = "systemuser";
    private Client client = mock(Client.class);
    private WebTarget webTarget = mock(WebTarget.class);
    private Builder requestBuilder = mock(Builder.class);
    private javax.ws.rs.core.Response response = mock(javax.ws.rs.core.Response.class);

    @BeforeClass
    public static void setupClass() {
        setProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME, "www.abac.com");
        setProperty(SYSTEMUSER_USERNAME, SYSTEMUSER);
        setProperty(CredentialConstants.SYSTEMUSER_PASSWORD, SYSTEMUSER_PASSWORD);
    }

    @Before
    public void setup(){
        when(client.target(anyString())).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(requestBuilder);
        when(requestBuilder.post(any())).thenReturn(response);
    }

    @Test
    public void returnsResponse() throws IOException, AbacException, NoSuchFieldException {
        gitt_response(200);
        gitt_responseEntity(getContentFromJsonFile("xacmlresponse.json"));

        final XacmlResponse actualXacmlResponse = abacService.askForPermission(MockXacmlRequest.getXacmlRequest(), client);

        final XacmlResponse expectedXacmlResponse = getExpectedXacmlResponse();

        assertThat(actualXacmlResponse, is(equalTo(expectedXacmlResponse)));
    }

    @Test(expected = AbacException.class)
    public void throwsExceptionAt500Error() throws IOException, AbacException, NoSuchFieldException {
        gitt_response(INTERNAL_SERVER_ERROR_500);
        abacService.askForPermission(MockXacmlRequest.getXacmlRequest(), client);
    }

    @Test(expected = ClientErrorException.class)
    public void throwsExceptionAt400Error() throws IOException, AbacException, NoSuchFieldException {
        gitt_response(UNAUTHORIZED_401);
        abacService.askForPermission(MockXacmlRequest.getXacmlRequest(), client);
    }

    private XacmlResponse getExpectedXacmlResponse() {

        final List<AttributeAssignment> attributeAssignments = new ArrayList<>();
        attributeAssignments.add(new AttributeAssignment("no.nav.abac.advice.fritekst", "Ikke tilgang"));

        final Advice advice = new Advice("no.nav.abac.advices.reason.deny_reason", attributeAssignments);

        final List<Advice> associatedAdvice = new ArrayList<>();
        associatedAdvice.add(advice);

        final Response response = new Response()
                .withDecision(Decision.Deny)
                .withAssociatedAdvice(associatedAdvice);
        final List<Response> responses = new ArrayList<>();
        responses.add(response);

        return new XacmlResponse()
                .withResponse(responses);
    }


    private void gitt_responseEntity(String contentFromJsonFile) {
        when(response.readEntity(String.class)).thenReturn(contentFromJsonFile);
    }

    private void gitt_response(int statusCode) {
        when(response.getStatus()).thenReturn(statusCode);
        when(response.getStatusInfo()).thenReturn(new javax.ws.rs.core.Response.StatusType() {
            @Override
            public int getStatusCode() {
                return statusCode;
            }

            @Override
            public Family getFamily() {
                return familyOf(statusCode);
            }

            @Override
            public String getReasonPhrase() {
                return Integer.toString(statusCode);
            }
        });
    }

}

