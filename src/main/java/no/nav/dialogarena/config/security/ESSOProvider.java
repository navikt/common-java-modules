package no.nav.dialogarena.config.security;

import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestUser;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.ws.rs.core.Response;
import java.net.HttpCookie;

public class ESSOProvider {

    private static final JerseyClient jerseyClient = new JerseyClientBuilder().build();

    public static HttpCookie getHttpCookie(String environment) {
        return new HttpCookie("nav-esso", getEssoCredentials(environment).sso);
    }

    public static ESSOCredentials getEssoCredentials(String environment) {
        TestUser testUser = FasitUtils.getTestUser("bruker_under_oppfolging");
        Response contentResponse = jerseyClient
                .target(String.format("https://itjenester-%s.oera.no/esso/identity/authenticate", environment))
                .queryParam("username", testUser.username)
                .queryParam("password", testUser.password)
                .request()
                .get();
        String openAMResponse = contentResponse.readEntity(String.class);
        String sso = openAMResponse.substring(openAMResponse.indexOf('=') + 1).trim();
        return new ESSOCredentials(testUser, sso);
    }

    public static class ESSOCredentials{
        public final TestUser testUser;
        public final String sso;

        public ESSOCredentials(TestUser testUser, String sso) {
            this.testUser = testUser;
            this.sso = sso;
        }
    }

}
