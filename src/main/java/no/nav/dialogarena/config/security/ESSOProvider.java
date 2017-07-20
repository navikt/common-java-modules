package no.nav.dialogarena.config.security;

import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestEnvironment;
import no.nav.dialogarena.config.fasit.TestUser;
import no.nav.dialogarena.config.util.Util;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpCookie;

import static org.eclipse.jetty.http.HttpMethod.POST;

public class ESSOProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ESSOProvider.class);

    public static HttpCookie getHttpCookie(TestEnvironment environment) {
        return getHttpCookie(environment.toString());
    }

    public static HttpCookie getHttpCookie(String environment) {
        return new HttpCookie("nav-esso", getEssoCredentials(environment).sso);
    }

    public static ESSOCredentials getEssoCredentials(String environment) {
        return getEssoCredentialsForUser("bruker_under_oppfolging", environment);
    }

    public static ESSOCredentials getEssoCredentialsForUser(String user, String environment){
        TestUser testUser = FasitUtils.getTestUser(user);
        return Util.httpClient(httpClient -> {
            String uri = String.format("https://itjenester-%s.oera.no/esso/identity/authenticate", environment);
            LOGGER.info(uri);
            ContentResponse contentResponse = httpClient
                    .newRequest(uri)
                    .method(POST)
                    .param("username", testUser.username)
                    .param("password", testUser.password)
                    .send();
            String openAMResponse = contentResponse.getContentAsString();
            if (contentResponse.getStatus() != 200) {
                throw new IllegalStateException(openAMResponse);
            }
            String sso = openAMResponse.substring(openAMResponse.indexOf('=') + 1).trim();
            return new ESSOCredentials(testUser, sso);
        });
    }

    public static class ESSOCredentials{
        public final TestUser testUser;
        public final String sso;

        private ESSOCredentials(TestUser testUser, String sso) {
            this.testUser = testUser;
            this.sso = sso;
        }
    }

}
