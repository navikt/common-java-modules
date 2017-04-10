package no.nav.dialogarena.config.security;

import org.junit.Test;

import java.net.HttpCookie;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;


public class ESSOProviderIntegrationTest {

    @Test
    public void getHttpCookie() {
        HttpCookie httpCookie = ESSOProvider.getHttpCookie("t6");
        assertThat(httpCookie.getName(), equalTo("nav-esso"));
        assertThat(httpCookie.getValue(), notNullValue());
    }

}