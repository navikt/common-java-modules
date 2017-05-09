package no.nav.dialogarena.config.security;

import org.junit.Test;

import java.net.HttpCookie;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class ISSOProviderIntegrationTest {

    @Test
    public void getISSOCookies() {
        List<HttpCookie> issoCookies = ISSOProvider.getISSOCookies();

        assertThat(issoCookies.size(), is(2));
        Set<String> cookieNames = issoCookies.stream().map(HttpCookie::getName).collect(Collectors.toSet());
        assertThat(cookieNames, equalTo(ISSOProvider.ISSO_COOKIE_NAMES));
    }

    @Test
    public void getISSOToken() {
        assertThat(ISSOProvider.getISSOToken(), notNullValue());
    }

}