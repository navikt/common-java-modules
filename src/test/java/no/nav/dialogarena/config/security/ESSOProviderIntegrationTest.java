package no.nav.dialogarena.config.security;

import org.junit.Test;

import java.net.HttpCookie;

import static no.nav.dialogarena.config.fasit.TestEnvironment.Q6;
import static no.nav.dialogarena.config.fasit.TestEnvironment.T6;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;


public class ESSOProviderIntegrationTest {

    @Test
    public void getHttpCookie() {
        sjekkCookie(ESSOProvider.getHttpCookie(T6));
        sjekkCookie(ESSOProvider.getHttpCookie(Q6));
    }

    @Test
    public void getEssoCredentialsForUser() {
        sjekkCredentials(ESSOProvider.getEssoCredentialsForUser(ESSOProvider.BRUKER_UNDER_OPPFOLGING, T6));
    }

    private void sjekkCredentials(ESSOProvider.ESSOCredentials essoCredentialsForUser) {
        sjekkCookie(essoCredentialsForUser.cookie);
    }

    private void sjekkCookie(HttpCookie httpCookie) {
        assertThat(httpCookie.getName(), equalTo("nav-esso"));
        assertThat(httpCookie.getValue(), notNullValue());
    }

}