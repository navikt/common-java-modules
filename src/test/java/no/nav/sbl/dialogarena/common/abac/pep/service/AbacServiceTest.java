package no.nav.sbl.dialogarena.common.abac.pep.service;

import no.nav.sbl.dialogarena.common.abac.pep.MockXacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.*;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbacServiceTest {

    @InjectMocks
    AbacService abacService;
    @org.mockito.Mock
    Abac abac;

    private static final String KEY_READ_TIMEOUT = "abac.bibliotek.readTimeout";
    private static final String KEY_CONNECTION_TIMEOUT = "abac.bibliotek.connectionTimeout";
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

    @After
    public void cleanUp() {
        System.clearProperty(KEY_CONNECTION_TIMEOUT);
        System.clearProperty(KEY_READ_TIMEOUT);
    }

    @Test
    public void returnsResponse() throws IOException, AbacException, NoSuchFieldException {

        when(abac.isAuthorized(any(RequestConfig.class), any(HttpPost.class), any(CredentialsProvider.class)))
                .thenReturn(prepareResponse(200, getContentFromJsonFile("xacmlresponse.json")));

        final XacmlResponse actualXacmlResponse = abacService.askForPermission(MockXacmlRequest.getXacmlRequest());

        final XacmlResponse expectedXacmlResponse = getExpectedXacmlResponse();

        assertThat(actualXacmlResponse, is(equalTo(expectedXacmlResponse)));

    }

    @Test(expected = AbacException.class)
    public void throwsExceptionAt500Error() throws IOException, AbacException, NoSuchFieldException {
        when(abac.isAuthorized(any(RequestConfig.class), any(HttpPost.class), any(CredentialsProvider.class)))
                .thenReturn(prepareResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, ""));

        abacService.askForPermission(MockXacmlRequest.getXacmlRequest());
    }

    @Test(expected = ClientErrorException.class)
    public void throwsExceptionAt400Error() throws IOException, AbacException, NoSuchFieldException {
        when(abac.isAuthorized(any(RequestConfig.class), any(HttpPost.class), any(CredentialsProvider.class)))
                .thenReturn(prepareResponse(HttpStatus.SC_UNAUTHORIZED, ""));

        abacService.askForPermission(MockXacmlRequest.getXacmlRequest());
    }

    @Test(expected = AbacException.class)
    public void throwsExceptionAtFailureAgainstABAC() throws IOException, AbacException, NoSuchFieldException {
        when(abac.isAuthorized(any(RequestConfig.class), any(HttpPost.class), any(CredentialsProvider.class)))
                .thenThrow(new IOException());

        abacService.askForPermission(MockXacmlRequest.getXacmlRequest());
    }

    @Test
    public void getsDefaultTimeoutWhenNoProperty() throws IOException, NoSuchFieldException, AbacException {
        final int expectedConnectionTimeout = 500;
        final int expectedReadTimeout = 1500;

        final RequestConfig requestConfig = getRequestConfigArgumentSentToAbac();

        assertThat(requestConfig.getConnectionRequestTimeout(), is(expectedConnectionTimeout));
        assertThat(requestConfig.getSocketTimeout(), is((expectedReadTimeout)));
        assertThat(requestConfig.getConnectTimeout(), is(expectedConnectionTimeout));

    }

    @Test
    public void getsTimeoutFromFromPropertyWhenPresent() throws IOException, NoSuchFieldException, AbacException {
        final String expectedConnectionTimeout = "1000";
        final String expectedReadTimeout = "2000";

        setProperty(KEY_CONNECTION_TIMEOUT, expectedConnectionTimeout);
        setProperty(KEY_READ_TIMEOUT, expectedReadTimeout);

        final RequestConfig requestConfig = getRequestConfigArgumentSentToAbac();

        assertThat(requestConfig.getConnectionRequestTimeout(), is(Integer.parseInt(expectedConnectionTimeout)));
        assertThat(requestConfig.getSocketTimeout(), is(Integer.parseInt(expectedReadTimeout)));
        assertThat(requestConfig.getConnectTimeout(), is(Integer.parseInt(expectedConnectionTimeout)));
    }

    @Test
    public void addsCredentialsToRequest() throws NoSuchFieldException, IOException, AbacException {
        final CredentialsProvider credentialsProvider = getCredentialArgumentSentToAbac();

        assertThat(credentialsProvider.getCredentials(AuthScope.ANY).getPassword(), is(SYSTEMUSER_PASSWORD));
        assertThat(credentialsProvider.getCredentials(AuthScope.ANY).getUserPrincipal().getName(), is(SYSTEMUSER));
    }


    private RequestConfig getRequestConfigArgumentSentToAbac() throws IOException, NoSuchFieldException, AbacException {
        when(abac.isAuthorized(any(RequestConfig.class), any(HttpPost.class), any(CredentialsProvider.class)))
                .thenReturn(prepareResponse(200, getContentFromJsonFile("xacmlresponse.json")));

        final ArgumentCaptor<RequestConfig> configCaptor = ArgumentCaptor.forClass(RequestConfig.class);

        abacService.askForPermission(MockXacmlRequest.getXacmlRequest());

        verify(abac).isAuthorized(configCaptor.capture(), any(HttpPost.class), any(CredentialsProvider.class));
        return configCaptor.getValue();
    }

    private CredentialsProvider getCredentialArgumentSentToAbac() throws IOException, NoSuchFieldException, AbacException {
        when(abac.isAuthorized(any(RequestConfig.class), any(HttpPost.class), any(CredentialsProvider.class)))
                .thenReturn(prepareResponse(200, getContentFromJsonFile("xacmlresponse.json")));

        final ArgumentCaptor<CredentialsProvider> credentialCaptor = ArgumentCaptor.forClass(CredentialsProvider.class);

        abacService.askForPermission(MockXacmlRequest.getXacmlRequest());

        verify(abac).isAuthorized(any(RequestConfig.class), any(HttpPost.class), credentialCaptor.capture());
        return credentialCaptor.getValue();
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

