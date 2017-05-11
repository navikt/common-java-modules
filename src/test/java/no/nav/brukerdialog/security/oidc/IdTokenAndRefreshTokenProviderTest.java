package no.nav.brukerdialog.security.oidc;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpPost;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static no.nav.brukerdialog.security.oidc.IdTokenAndRefreshTokenProvider.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class IdTokenAndRefreshTokenProviderTest {

    public static final String APPLICATION_NAME_PROPERTY_NAME = "applicationName";
    public static final String ISSO_HOST_URL_PROPERTY_NAME = "isso-host.url";
    public static final String ISSO_RP_USER_USERNAME_PROPERTY_NAME = "isso-rp-user.username";
    public static final String ISSO_RP_USER_PASSWORD_PROPERTY_NAME = "isso-rp-user.password";

    @Test
    public void createTokenRequest_fraSystemProperties() throws Exception {
        System.setProperty(APPLICATION_NAME_PROPERTY_NAME, "minapp");
        System.setProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME, "minbruker");
        System.setProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME, "mittpassord");
        System.setProperty(ISSO_HOST_URL_PROPERTY_NAME, "https://minhost.nav.no");

        HttpPost tokenRequest = new IdTokenAndRefreshTokenProvider().createTokenRequest("abcd", getUriInfo());

        assertThat(getEntityAsString(tokenRequest), equalTo("grant_type=authorization_code&realm=/&redirect_uri=https%3A%2F%2Fhost%3A1234%2Fminapp%2Ftjenester%2Flogin&code=abcd"));
        assertThat(tokenRequest.getHeaders(HttpHeaders.AUTHORIZATION)[0].getValue(), equalTo("Basic bWluYnJ1a2VyOm1pdHRwYXNzb3Jk"));
        assertThat(tokenRequest.getURI().toString(),equalTo("https://minhost.nav.no/access_token"));
    }

    private UriInfo getUriInfo() {
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        when(uriInfo.getBaseUri()).thenReturn(URI.create("https://host:1234/a/b/c"));
        return uriInfo;
    }

    private String getEntityAsString(HttpPost httpPost) throws Exception {
        return IOUtils.toString(httpPost.getEntity().getContent(), ENCODING);
    }

}
