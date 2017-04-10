package no.nav.dialogarena.config.security;

import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestUser;
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
        TestUser kerberos_test_token = FasitUtils.getTestUser("kerberos_test_token");
        List<HttpCookie> issoCookies = ISSOProvider.getISSOCookies(kerberos_test_token.password, "https://app-t6.adeo.no/veilarbpersonflatefs/tjenester/login");

        assertThat(issoCookies.size(), is(2));
        Set<String> cookieNames = issoCookies.stream().map(HttpCookie::getName).collect(Collectors.toSet());
        assertThat(cookieNames, equalTo(ISSOProvider.ISSO_COOKIE_NAMES));
    }

}