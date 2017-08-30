package no.nav.dialogarena.config.security;

import no.nav.dialogarena.config.DevelopmentSecurity;
import org.junit.Test;

import java.net.HttpCookie;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.dialogarena.config.fasit.TestEnvironment.Q6;
import static no.nav.dialogarena.config.security.ISSOProvider.getDefaultRedirectUrl;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class ISSOProviderIntegrationTest {

    private static final String DEFAULT_REDIRECT_URL = getDefaultRedirectUrl();
    private static final String REDIRECT_URL_Q = DevelopmentSecurity.getRedirectUrl(Q6.toString());

    @Test
    public void getISSOCookies() {
        sjekkIssoCookies(ISSOProvider.getISSOCookies());
        sjekkIssoCookies(ISSOProvider.getISSOCookies(ISSOProvider.getPriveligertVeileder()));
        sjekkIssoCookies(ISSOProvider.getISSOCookies(ISSOProvider.getTestAuthorization(), DEFAULT_REDIRECT_URL));
        sjekkIssoCookies(ISSOProvider.getISSOCookies(ISSOProvider.getTestAuthorization(), DEFAULT_REDIRECT_URL, ISSOProvider.getPriveligertVeileder()));
    }

    @Test
    public void getISSOCookies_q() {
        sjekkIssoCookies(ISSOProvider.getISSOCookies(REDIRECT_URL_Q, Q6));
    }

    @Test
    public void getISSOToken() {
        sjekkIssoToken(ISSOProvider.getISSOToken());
        sjekkIssoToken(ISSOProvider.getISSOToken(ISSOProvider.getTestUser()));
        sjekkIssoToken(ISSOProvider.getISSOToken(ISSOProvider.getTestUser(), DEFAULT_REDIRECT_URL));
        sjekkIssoToken(ISSOProvider.getISSOToken(ISSOProvider.getTestUser(), DEFAULT_REDIRECT_URL));
        sjekkIssoToken(ISSOProvider.getISSOToken(ISSOProvider.getTestUser(), DEFAULT_REDIRECT_URL, ISSOProvider.getTestAuthorization()));
    }

    @Test
    public void getISSOToken_q() {
        sjekkIssoToken(ISSOProvider.getISSOToken(ISSOProvider.getTestUser(Q6), REDIRECT_URL_Q));
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