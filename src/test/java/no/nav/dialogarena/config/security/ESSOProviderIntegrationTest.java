package no.nav.dialogarena.config.security;

import no.nav.dialogarena.config.fasit.TestEnvironment;
import no.nav.modig.security.loginmodule.userinfo.openam.OpenAMUserInfoService;
import org.apache.http.conn.UnsupportedSchemeException;
import org.junit.Ignore;
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
    public void skal_hente_cookie_fra_default_environment() {
        sjekkCookie(ESSOProvider.getHttpCookie());
    }

    @Test
    public void skal_hente_cookie_fra_t6() {
        sjekkCookie(ESSOProvider.getHttpCookie(T6));
    }

    @Test
    public void skal_hente_cookie_fra_privat_bruker_i_t6() {
        sjekkCookie(ESSOProvider.getHttpCookie(T6, PRIVAT_BRUKER));
    }

    @Test
    public void skal_hente_cookie_fra_q6() {
        sjekkCookie(ESSOProvider.getHttpCookie(Q6));
    }

    @Test
    public void skal_hente_cookie_fra_q0() {
        sjekkCookie(ESSOProvider.getHttpCookie(Q0));
    }

    @Ignore
    @Test
    public void should_login_to_level_4_in_t6() throws UnsupportedSchemeException {
        sjekkNiva4(T6, BRUKER_UNDER_OPPFOLGING);
    }

    @Ignore
    @Test
    public void should_login_to_level_4_in_q6() throws UnsupportedSchemeException {
        sjekkNiva4(Q6, BRUKER_UNDER_OPPFOLGING);
    }

    @Ignore
    @Test
    public void should_login_privat_bruker_to_level_4_in_q6() throws UnsupportedSchemeException {
        sjekkNiva4(T6, PRIVAT_BRUKER);
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