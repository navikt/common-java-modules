package no.nav.dialogarena.config.security;

import org.junit.Test;

import java.net.HttpCookie;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.dialogarena.config.fasit.TestEnvironment.Q6;
import static no.nav.dialogarena.config.security.ISSOProvider.KJENT_LOGIN_ADRESSE;
import static no.nav.dialogarena.config.security.ISSOProvider.KJENT_LOGIN_ADRESSE_Q;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;

public class ISSOProviderIntegrationTest {

    @Test
    public void getISSOCookies() {
        sjekkIssoCookies(ISSOProvider.getISSOCookies());
        sjekkIssoCookies(ISSOProvider.getISSOCookies(ISSOProvider.getTestAuthorization(), KJENT_LOGIN_ADRESSE));
        sjekkIssoCookies(ISSOProvider.getISSOCookies(ISSOProvider.getTestAuthorization(), KJENT_LOGIN_ADRESSE, ISSOProvider.getPriveligertVeileder()));
    }

    @Test
    public void getISSOCookies_q() {
        // godtar at dette ikke fungerer før vi er i q med veilarblogin
        assumeFalse(LocalDate.of(2017, 8, 26).isAfter(LocalDate.now()));

        sjekkIssoCookies(ISSOProvider.getISSOCookies(KJENT_LOGIN_ADRESSE_Q, Q6));
    }

    @Test
    public void getISSOToken() {
        sjekkIssoToken(ISSOProvider.getISSOToken());
        sjekkIssoToken(ISSOProvider.getISSOToken(ISSOProvider.getTestUser()));
        sjekkIssoToken(ISSOProvider.getISSOToken(ISSOProvider.getTestUser(), KJENT_LOGIN_ADRESSE));
        sjekkIssoToken(ISSOProvider.getISSOToken(ISSOProvider.getTestUser(), KJENT_LOGIN_ADRESSE));
        sjekkIssoToken(ISSOProvider.getISSOToken(ISSOProvider.getTestUser(), KJENT_LOGIN_ADRESSE, ISSOProvider.getTestAuthorization()));
    }

    @Test
    public void getISSOToken_q() {
        // godtar at dette ikke fungerer før vi er i q med veilarblogin
        assumeFalse(LocalDate.of(2017, 8, 26).isAfter(LocalDate.now()));

        sjekkIssoToken(ISSOProvider.getISSOToken(ISSOProvider.getTestUser(Q6), KJENT_LOGIN_ADRESSE_Q));
    }

    private void sjekkIssoToken(String issoToken) {
        assertThat(issoToken, notNullValue());
    }

    private void sjekkIssoCookies(List<HttpCookie> issoCookies) {
        assertThat(issoCookies.size(), is(2));
        Set<String> cookieNames = issoCookies.stream().map(HttpCookie::getName).collect(Collectors.toSet());
        assertThat(cookieNames, equalTo(ISSOProvider.ISSO_COOKIE_NAMES));
    }


}