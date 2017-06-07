package no.nav.sbl.dialogarena.common.abac.pep.service;

import no.nav.sbl.dialogarena.common.abac.pep.MockXacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.*;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.ClientErrorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.setProperty;
import static no.nav.sbl.dialogarena.common.abac.TestUtils.getContentFromJsonFile;
import static no.nav.sbl.dialogarena.common.abac.TestUtils.prepareResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbacServiceTest {

    @Mock
    Abac abac;
    @Mock
    CloseableHttpClient httpClient;
    @InjectMocks
    AbacService abacService;


    private static final String SYSTEMUSER_PASSWORD = "systemuserpassword";
    private static final String SYSTEMUSER = "systemuser";

    @BeforeClass
    public static void setup() {
        setProperty("ldap.url", "www.something.com");
        setProperty("ldap.username", "username");
        setProperty("ldap.password", "supersecrectpassword");
        setProperty("abac.endpoint.url", "www.abac.com");
        setProperty("no.nav.abac.systemuser.username", SYSTEMUSER);
        setProperty("no.nav.abac.systemuser.password", SYSTEMUSER_PASSWORD);
    }

    @Test
    public void returnsResponse() throws IOException, AbacException, NoSuchFieldException {
        CloseableHttpResponse response = prepareResponse(200, getContentFromJsonFile("xacmlresponse.json"));
        when(abac.isAuthorized(any(CloseableHttpClient.class), any(HttpPost.class))).thenReturn(response);

        final XacmlResponse actualXacmlResponse = abacService.askForPermission(MockXacmlRequest.getXacmlRequest());

        final XacmlResponse expectedXacmlResponse = getExpectedXacmlResponse();

        assertThat(actualXacmlResponse, is(equalTo(expectedXacmlResponse)));

    }

    @Test(expected = AbacException.class)
    public void throwsExceptionAt500Error() throws IOException, AbacException, NoSuchFieldException {
        CloseableHttpResponse response = prepareResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "");
        when(abac.isAuthorized(any(CloseableHttpClient.class), any(HttpPost.class))).thenReturn(response);

        abacService.askForPermission(MockXacmlRequest.getXacmlRequest());
    }

    @Test(expected = ClientErrorException.class)
    public void throwsExceptionAt400Error() throws IOException, AbacException, NoSuchFieldException {
        CloseableHttpResponse response = prepareResponse(HttpStatus.SC_UNAUTHORIZED, "");
        when(abac.isAuthorized(any(CloseableHttpClient.class), any(HttpPost.class))).thenReturn(response);

        abacService.askForPermission(MockXacmlRequest.getXacmlRequest());
    }

    @Test(expected = AbacException.class)
    public void throwsExceptionAtFailureAgainstABAC() throws IOException, AbacException, NoSuchFieldException {
        when(abac.isAuthorized(any(CloseableHttpClient.class), any(HttpPost.class)))
                .thenThrow(new IOException());

        abacService.askForPermission(MockXacmlRequest.getXacmlRequest());
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


}

