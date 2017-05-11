package no.nav.brukerdialog.security.oidc;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpPost;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.oidc.IdTokenAndRefreshTokenProvider.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class IdTokenAndRefreshTokenProviderTest {

    public static final String APPLICATION_NAME_PROPERTY_NAME = "applicationName";

    @Before
    public void setup(){
        setProperty(APPLICATION_NAME_PROPERTY_NAME, "minapp"); // TODO denne er kjip siden den er statisk på innsiden av klassen

        clearProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME);
        clearProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME);
        clearProperty(ISSO_HOST_URL_PROPERTY_NAME);
    }

    @Test
    public void createTokenRequest_fraSystemProperties() throws Exception {
        setProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME, "minbruker");
        setProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME, "mittpassord");
        setProperty(ISSO_HOST_URL_PROPERTY_NAME, "https://minhost.nav.no");

        HttpPost tokenRequest = new IdTokenAndRefreshTokenProvider().createTokenRequest("abcd", getUriInfo("https://host:1234/a/b/c"));

        assertThat(getEntityAsString(tokenRequest), equalTo("grant_type=authorization_code&realm=/&redirect_uri=https%3A%2F%2Fhost%3A1234%2Fminapp%2Ftjenester%2Flogin&code=abcd"));
        assertThat(tokenRequest.getHeaders(HttpHeaders.AUTHORIZATION)[0].getValue(), equalTo("Basic bWluYnJ1a2VyOm1pdHRwYXNzb3Jk"));
        assertThat(tokenRequest.getURI().toString(),equalTo("https://minhost.nav.no/access_token"));
    }

    @Test
    public void createTokenRequest_fraParametre() throws Exception {
        Parameters parameters = Parameters.builder()
                .redirectUrl("/min/redirect/url")
                .host("https://minparameterhost.nav.no")
                .username("minparameterbruker")
                .password("mittparameterpassord")
                .build();

        HttpPost tokenRequest = new IdTokenAndRefreshTokenProvider(parameters).createTokenRequest("abcd", getUriInfo("ftp://param:5678/a/b/c"));

        assertThat(getEntityAsString(tokenRequest), equalTo("grant_type=authorization_code&realm=/&redirect_uri=ftp%3A%2F%2Fparam%3A5678%2Fmin%2Fredirect%2Furl&code=abcd"));
        assertThat(tokenRequest.getHeaders(HttpHeaders.AUTHORIZATION)[0].getValue(), equalTo("Basic bWlucGFyYW1ldGVyYnJ1a2VyOm1pdHRwYXJhbWV0ZXJwYXNzb3Jk"));
        assertThat(tokenRequest.getURI().toString(),equalTo("https://minparameterhost.nav.no/access_token"));
    }

    private UriInfo getUriInfo(String baseUri) {
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        when(uriInfo.getBaseUri()).thenReturn(URI.create(baseUri));
        return uriInfo;
    }

    private String getEntityAsString(HttpPost httpPost) throws Exception {
        return IOUtils.toString(httpPost.getEntity().getContent(), ENCODING);
    }

}
