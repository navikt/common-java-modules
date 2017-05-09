package no.nav.dialogarena.config.security;

import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.dialogarena.config.fasit.TestUser;
import org.junit.Test;

import java.net.HttpCookie;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.dialogarena.config.DevelopmentSecurity.DEFAULT_ISSO_RP_USER;
import static no.nav.dialogarena.config.security.ISSOProvider.KJENT_LOGIN_ADRESSE;
import static no.nav.dialogarena.config.util.Util.setProperty;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class ISSOProviderIntegrationTest {


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
        ServiceUser issoServiceUser = FasitUtils.getServiceUser(DEFAULT_ISSO_RP_USER, "veilarbsituasjon", "t6");
        String issoToken = ISSOProvider.getISSOToken(issoServiceUser, kerberos_test_token.password);
        assertThat(issoToken, notNullValue());
    }

}