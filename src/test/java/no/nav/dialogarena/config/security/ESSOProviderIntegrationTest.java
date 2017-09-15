package no.nav.dialogarena.config.security;

import no.nav.dialogarena.config.fasit.TestEnvironment;
import no.nav.modig.security.loginmodule.userinfo.openam.OpenAMUserInfoService;
import org.apache.http.conn.UnsupportedSchemeException;
import org.junit.Test;

import java.net.HttpCookie;
import java.net.URI;

import static no.nav.dialogarena.config.fasit.TestEnvironment.*;
import static no.nav.dialogarena.config.security.ESSOProvider.BRUKER_UNDER_OPPFOLGING;
import static no.nav.dialogarena.config.security.ESSOProvider.PRIVAT_BRUKER;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;


public class ESSOProviderIntegrationTest {

    @Test
    public void getHttpCookie() {
        sjekkCookie(ESSOProvider.getHttpCookie(T6));
        sjekkCookie(ESSOProvider.getHttpCookie(Q4));
        sjekkCookie(ESSOProvider.getHttpCookie(T4, PRIVAT_BRUKER));
    }

    @Test
    public void niva4() throws UnsupportedSchemeException {
        sjekkNiva4(T6, BRUKER_UNDER_OPPFOLGING);
        sjekkNiva4(Q4, BRUKER_UNDER_OPPFOLGING);
        sjekkNiva4(T4, PRIVAT_BRUKER);
    }

    @Test
    public void getEssoCredentialsForUser() {
        sjekkCredentials(ESSOProvider.getEssoCredentialsForUser(BRUKER_UNDER_OPPFOLGING, T6));
        sjekkCredentials(ESSOProvider.getEssoCredentialsForUser(PRIVAT_BRUKER, T4));
        sjekkCredentials(ESSOProvider.getEssoCredentialsForUser(BRUKER_UNDER_OPPFOLGING, Q4));
    }

    private void sjekkNiva4(TestEnvironment t4, String privatBruker) throws UnsupportedSchemeException {
        String sso = ESSOProvider.getHttpCookie(t4, privatBruker).getValue();
        OpenAMUserInfoService openAMUserInfoService = new OpenAMUserInfoService(URI.create(String.format("https://itjenester-%s.oera.no/esso", t4.toString())));
        assertThat(openAMUserInfoService.getUserInfo(sso).getAuthLevel(), is(4));
    }

    private void sjekkCredentials(ESSOProvider.ESSOCredentials essoCredentialsForUser) {
        sjekkCookie(essoCredentialsForUser.cookie);
    }

    private void sjekkCookie(HttpCookie httpCookie) {
        assertThat(httpCookie.getName(), equalTo("nav-esso"));
        assertThat(httpCookie.getValue(), notNullValue());
    }

}