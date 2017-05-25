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

    @Before
    public void setup(){
        clearProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME);
        clearProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME);
        clearProperty(ISSO_HOST_URL_PROPERTY_NAME);
    }

    @Test
    public void createTokenRequest_fraSystemProperties() throws Exception {
        setProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME, "minbruker");
        setProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME, "mittpassord");
        setProperty(ISSO_HOST_URL_PROPERTY_NAME, "https://minhost.nav.no");

        HttpPost tokenRequest = new IdTokenAndRefreshTokenProvider().createTokenRequest("abcd", "https://minhost.nav.no/minapp");

        assertThat(getEntityAsString(tokenRequest), equalTo("grant_type=authorization_code&realm=/&redirect_uri=https%3A%2F%2Fminhost.nav.no%2Fminapp&code=abcd"));
        assertThat(tokenRequest.getHeaders(HttpHeaders.AUTHORIZATION)[0].getValue(), equalTo("Basic bWluYnJ1a2VyOm1pdHRwYXNzb3Jk"));
        assertThat(tokenRequest.getURI().toString(),equalTo("https://minhost.nav.no/access_token"));
    }

    @Test
    public void createTokenRequest_fraParametre() throws Exception {
        Parameters parameters = Parameters.builder()
                .host("https://minparameterhost.nav.no")
                .username("minparameterbruker")
                .password("mittparameterpassord")
                .build();

        HttpPost tokenRequest = new IdTokenAndRefreshTokenProvider(parameters).createTokenRequest("abcd", "ftp://param:5678/a/b/c");

        assertThat(getEntityAsString(tokenRequest), equalTo("grant_type=authorization_code&realm=/&redirect_uri=ftp%3A%2F%2Fparam%3A5678%2Fa%2Fb%2Fc&code=abcd"));
        assertThat(tokenRequest.getHeaders(HttpHeaders.AUTHORIZATION)[0].getValue(), equalTo("Basic bWlucGFyYW1ldGVyYnJ1a2VyOm1pdHRwYXJhbWV0ZXJwYXNzb3Jk"));
        assertThat(tokenRequest.getURI().toString(),equalTo("https://minparameterhost.nav.no/access_token"));
    }

    private String getEntityAsString(HttpPost httpPost) throws Exception {
        return IOUtils.toString(httpPost.getEntity().getContent(), ENCODING);
    }

}
