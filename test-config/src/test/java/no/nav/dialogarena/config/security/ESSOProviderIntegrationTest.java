package no.nav.dialogarena.config.security;

import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestEnvironment;
import no.nav.modig.security.loginmodule.userinfo.openam.OpenAMUserInfoService;
import org.apache.http.conn.UnsupportedSchemeException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpCookie;
import java.net.URI;

import static no.nav.dialogarena.config.fasit.TestEnvironment.*;
import static no.nav.dialogarena.config.security.ESSOProvider.*;
import static no.nav.sbl.rest.RestUtils.withClient;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;


public class ESSOProviderIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ESSOProviderIntegrationTest.class);

    @Test
    public void skal_hente_cookie_fra_default_environment() {
        assumeAlive(FasitUtils.getDefaultTestEnvironment());
        sjekkCookie(ESSOProvider.getHttpCookie());
    }

    @Test
    public void skal_hente_cookie_fra_t6() {
        assumeAlive(T6);
        sjekkCookie(ESSOProvider.getHttpCookie(T6));
    }

    @Test
    public void skal_hente_cookie_fra_privat_bruker_i_t6() {
        assumeAlive(T6);
        sjekkCookie(ESSOProvider.getHttpCookie(T6, PRIVAT_BRUKER));
    }

    @Test
    public void skal_hente_cookie_fra_q6() {
        assumeAlive(Q6);
        sjekkCookie(ESSOProvider.getHttpCookie(Q6));
    }

    @Test
    public void skal_hente_cookie_fra_q0() {
        assumeAlive(Q0);
        sjekkCookie(ESSOProvider.getHttpCookie(Q0));
    }

    @Test
    public void should_login_to_level_4_in_t6() throws UnsupportedSchemeException {
        assumeAlive(T6);
        sjekkNiva4(T6, BRUKER_UNDER_OPPFOLGING);
    }

    @Test
    public void should_login_to_level_4_in_q6() throws UnsupportedSchemeException {
        assumeAlive(Q6);
        sjekkNiva4(Q6, BRUKER_UNDER_OPPFOLGING);
    }

    @Test
    public void should_login_privat_bruker_to_level_4_in_q6() throws UnsupportedSchemeException {
        assumeAlive(T6);
        sjekkNiva4(T6, PRIVAT_BRUKER);
    }

    @Test
    public void getEssoCredentialsForUser_t6() {
        assumeAlive(T6);
        sjekkCredentials(ESSOProvider.getEssoCredentialsForUser(BRUKER_UNDER_OPPFOLGING, T6));
    }

    @Test
    public void getEssoCredentialsForUser_t4() {
        assumeAlive(T4);
        sjekkCredentials(ESSOProvider.getEssoCredentialsForUser(PRIVAT_BRUKER, T4));
    }

    @Test
    public void getEssoCredentialsForUser_q4() {
        assumeAlive(Q4);
        sjekkCredentials(ESSOProvider.getEssoCredentialsForUser(BRUKER_UNDER_OPPFOLGING, Q4));
    }

    private void assumeAlive(TestEnvironment testEnvironment) {
        assumeTrue(isAlive(testEnvironment));
    }

    private Boolean isAlive(TestEnvironment testEnvironment) {
        return withClient(client -> {
            try {
                int status = client.target(essoBaseUrl(testEnvironment.toString())).request().get().getStatus();
                return status == 302; // expecting redirect to login page
            } catch (Exception e) {
                LOGGER.warn(e.getMessage(), e);
                return false;
            }
        });
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