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

    private static final String KJENT_LOGIN_ADRESSE = "https://app-t6.adeo.no/veilarbpersonflatefs/tjenester/login";

    @Test
    public void getISSOCookies() {
        TestUser kerberos_test_token = FasitUtils.getTestUser("kerberos_test_token");
        List<HttpCookie> issoCookies = ISSOProvider.getISSOCookies(kerberos_test_token.password, KJENT_LOGIN_ADRESSE);

        assertThat(issoCookies.size(), is(2));
        Set<String> cookieNames = issoCookies.stream().map(HttpCookie::getName).collect(Collectors.toSet());
        assertThat(cookieNames, equalTo(ISSOProvider.ISSO_COOKIE_NAMES));
    }

    @Test
    public void getISSOToken() {
        TestUser kerberos_test_token = FasitUtils.getTestUser("kerberos_test_token");
        String issoToken = ISSOProvider.getISSOToken(kerberos_test_token.password, KJENT_LOGIN_ADRESSE);
        assertThat(issoToken, notNullValue());
    }

}