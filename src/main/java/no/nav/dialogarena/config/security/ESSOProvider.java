package no.nav.dialogarena.config.security;

import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestEnvironment;
import no.nav.dialogarena.config.fasit.TestUser;
import no.nav.dialogarena.config.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.net.HttpCookie;
import java.util.Map;


import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

public class ESSOProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ESSOProvider.class);

    public static final String BRUKER_UNDER_OPPFOLGING = "bruker_under_oppfolging";
    public static final String PRIVAT_BRUKER = "privat_bruker";
    public static final String NAV_ESSO_COOKIE_NAVN = "nav-esso";

    public static HttpCookie getHttpCookie(TestEnvironment environment) {
        return getHttpCookie(environment.toString());
    }

    public static HttpCookie getHttpCookie(String environment) {
        return getHttpCookie(BRUKER_UNDER_OPPFOLGING, environment);
    }

    public static HttpCookie getHttpCookie(String brukerUnderOppfolging, String environment) {
        return getEssoCredentialsForUser(brukerUnderOppfolging, environment).cookie;
    }

    public static HttpCookie getHttpCookie(TestEnvironment environment, String brukerUnderOppfolging) {
        return getEssoCredentialsForUser(brukerUnderOppfolging, environment.toString()).cookie;
    }

    public static ESSOCredentials getEssoCredentials(String environment) {
        return getEssoCredentialsForUser(BRUKER_UNDER_OPPFOLGING, environment);
    }

    public static ESSOCredentials getEssoCredentialsForUser(String user, TestEnvironment testEnvironment) {
        return getEssoCredentialsForUser(user, testEnvironment.toString());
    }

    public static ESSOCredentials getEssoCredentialsForUser(String user, String environment) {
        TestUser testUser = FasitUtils.getTestUser(user, environment);
        return Util.httpClient(httpClient -> {
            String uri = String.format("https://tjenester-%s.nav.no/esso/UI/Login?service=level4Service&goto=https://tjenester-%s.nav.no/aktivitetsplan/", environment, environment);
            LOGGER.info(uri);
            MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
            form.putSingle("IDToken1", testUser.username);
            form.putSingle("IDToken2", testUser.password);
            Response response = httpClient
                    .target(uri)
                    .request()
                    .post(Entity.form(form));
            if (response.getStatus() != 302) {
                throw new IllegalStateException(response.readEntity(String.class));
            }
            Map<String, NewCookie> cookies = response.getCookies();
            NewCookie cookie = cookies.get(NAV_ESSO_COOKIE_NAVN);
            if (cookie == null) {
                // TODO
                throw new IllegalStateException();
            }
            return new ESSOCredentials(testUser, cookie.getValue());
        });
    }

    public static class ESSOCredentials {
        public final TestUser testUser;
        public final String sso;
        public final HttpCookie cookie;

        private ESSOCredentials(TestUser testUser, String sso) {
            this.testUser = testUser;
            this.sso = sso;
            this.cookie = new HttpCookie(NAV_ESSO_COOKIE_NAVN, sso);
        }
    }

}
