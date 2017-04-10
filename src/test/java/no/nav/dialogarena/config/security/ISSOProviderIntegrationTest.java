package no.nav.dialogarena.config.security;

import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestUser;
import org.junit.Test;

import java.net.HttpCookie;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class ISSOProviderIntegrationTest {

    @Test
    public void getISSOCookies() {
        TestUser kerberos_test_token = FasitUtils.getTestUser("kerberos_test_token");
        List<HttpCookie> issoCookies = ISSOProvider.getISSOCookies(kerberos_test_token.password, "https://app-t6.adeo.no/veilarbpersonflatefs/tjenester/login");

        assertThat(issoCookies.size(), is(2));

        HttpCookie refreshTokenCookie = issoCookies.get(0);
        assertThat(refreshTokenCookie.getName(), equalTo("refresh_token"));
        assertThat(refreshTokenCookie.getName(), notNullValue());

        HttpCookie idTokenCookie = issoCookies.get(1);
        assertThat(idTokenCookie.getName(), equalTo("ID_token"));
        assertThat(idTokenCookie.getName(), notNullValue());
    }

}