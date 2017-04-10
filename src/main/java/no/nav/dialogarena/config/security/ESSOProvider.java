package no.nav.dialogarena.config.security;

import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestUser;
import no.nav.dialogarena.config.util.Util;
import org.eclipse.jetty.client.api.ContentResponse;

import java.net.HttpCookie;

public class ESSOProvider {

    public static HttpCookie getHttpCookie(String environment) {
        return new HttpCookie("nav-esso", getEssoCredentials(environment).sso);
    }

    public static ESSOCredentials getEssoCredentials(String environment) {
        TestUser testUser = FasitUtils.getTestUser("bruker_under_oppfolging");
        return Util.httpClient(httpClient -> {
            ContentResponse contentResponse = httpClient
                    .newRequest(String.format("https://itjenester-%s.oera.no/esso/identity/authenticate", environment))
                    .param("username", testUser.username)
                    .param("password", testUser.password)
                    .send();
            String openAMResponse = contentResponse.getContentAsString();
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
