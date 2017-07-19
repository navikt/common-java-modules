package no.nav.dialogarena.config.security;

import org.junit.Test;

import java.net.HttpCookie;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.dialogarena.config.security.ISSOProvider.KJENT_LOGIN_ADRESSE;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class ISSOProviderIntegrationTest {

    @Test
    public void getISSOCookies() {
        sjekkIssoCookies(ISSOProvider.getISSOCookies());
        sjekkIssoCookies(ISSOProvider.getISSOCookies(ISSOProvider.getTestAuthorization(), KJENT_LOGIN_ADRESSE));
        sjekkIssoCookies(ISSOProvider.getISSOCookies(ISSOProvider.getTestAuthorization(), KJENT_LOGIN_ADRESSE, ISSOProvider.getPriveligertVeileder()));
    }

    @Test
    public void getISSOToken() {
        sjekkIssoToken(ISSOProvider.getISSOToken());
        sjekkIssoToken(ISSOProvider.getISSOToken(ISSOProvider.getTestUser()));
        sjekkIssoToken(ISSOProvider.getISSOToken(ISSOProvider.getTestUser(),ISSOProvider.getTestAuthorization()));
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