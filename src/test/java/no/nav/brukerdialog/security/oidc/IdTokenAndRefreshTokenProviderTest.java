package no.nav.brukerdialog.security.oidc;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.net.URI;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.oidc.IdTokenAndRefreshTokenProvider.Parameters;
import static no.nav.sbl.rest.RestUtils.withClient;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class IdTokenAndRefreshTokenProviderTest {

    private static final Logger LOG = LoggerFactory.getLogger(IdTokenProvider.class);

    @Before
    public void setup() {
        clearProperty("isso-rp-user.username");
        clearProperty("isso-rp-user.password");
        clearProperty("isso-host.url");
    }

    @Test
    public void createTokenRequest_fraSystemProperties() throws Exception {
        setProperty("isso-rp-user.username", "minbruker");
        setProperty("isso-rp-user.password", "mittpassord");
        setProperty("isso-host.url", "https://minhost.nav.no");

        withClient(client -> {
            RequestCaptor captor = new RequestCaptor();
            client.register(captor);

            try {
                new IdTokenAndRefreshTokenProvider().createTokenRequest("abcd", "https://minhost.nav.no/minapp", client);
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
            }

            assertThat(captor.entity, equalTo("grant_type=authorization_code&realm=/&redirect_uri=https%3A%2F%2Fminhost.nav.no%2Fminapp&code=abcd"));
            assertThat(captor.headers.get(HttpHeaders.AUTHORIZATION).get(0), equalTo("Basic bWluYnJ1a2VyOm1pdHRwYXNzb3Jk"));
            assertThat(captor.uri.toString(), equalTo("https://minhost.nav.no/access_token"));
            return captor;
        });
    }

    @Test
    public void createTokenRequest_fraParametre() throws Exception {
        Parameters parameters = Parameters.builder()
                .host("https://minparameterhost.nav.no")
                .username("minparameterbruker")
                .password("mittparameterpassord")
                .build();

        withClient(client -> {
            RequestCaptor captor = new RequestCaptor();
            client.register(captor);

            try {
                new IdTokenAndRefreshTokenProvider(parameters).createTokenRequest("abcd", "ftp://param:5678/a/b/c", client);
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
            }

            assertThat(captor.entity, equalTo("grant_type=authorization_code&realm=/&redirect_uri=ftp%3A%2F%2Fparam%3A5678%2Fa%2Fb%2Fc&code=abcd"));
            assertThat(captor.headers.get(HttpHeaders.AUTHORIZATION).get(0), equalTo("Basic bWlucGFyYW1ldGVyYnJ1a2VyOm1pdHRwYXJhbWV0ZXJwYXNzb3Jk"));
            assertThat(captor.uri.toString(),equalTo("https://minparameterhost.nav.no/access_token"));

            return captor;
        });
    }

    private static class RequestCaptor implements ClientRequestFilter {

        private Object entity;
        private MultivaluedMap<String, Object> headers;
        private URI uri;

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            uri = requestContext.getUri();
            entity = requestContext.getEntity();
            headers = requestContext.getHeaders();
        }
    }
}
