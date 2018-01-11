package no.nav.apiapp.rest;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static no.nav.apiapp.rest.NavCorsFilter.CORS_ALLOWED_ORIGINS;
import static no.nav.apiapp.rest.NavCorsFilter.getAllowedOrigins;
import static no.nav.apiapp.rest.NavCorsFilter.validOrigin;
import static org.assertj.core.api.Assertions.assertThat;


class NavCorsFilterTest {

    @Test
    public void validOrigin_() {
        assertInvalidOrigin("origin", null);
        assertInvalidOrigin("", null);
        assertInvalidOrigin(null, null, null);
        assertInvalidOrigin("abcd.nav.no");
        assertInvalidOrigin("nav.no", "evil.com");
        assertInvalidOrigin("evil.com", "", null);

        assertValidOrigin("abcd.nav.no", "nav.no");
        assertValidOrigin("abcd.nav.no", "nav.no");
        assertValidOrigin("abcd.nav.no", "oera.no", "nav.no");
        assertValidOrigin("abcd.nav.no", "oera.no", "", null, "nav.no");
    }

    private void assertValidOrigin(String origin, String... validOrigins) {
        assertThat(validOrigin(origin, Arrays.asList(validOrigins))).isTrue();
    }

    private void assertInvalidOrigin(String origin, String... validOrigins) {
        assertThat(validOrigin(origin, validOrigins != null ? Arrays.asList(validOrigins) : Collections.emptyList())).isFalse();
    }

    @Test
    public void hentKommaseparertListe() {
        assertThat(getAllowedOrigins()).isEmpty();
        System.setProperty(CORS_ALLOWED_ORIGINS, "");
        assertThat(getAllowedOrigins()).isEmpty();
        System.setProperty(CORS_ALLOWED_ORIGINS, "nav.no,oera.no");
        assertThat(getAllowedOrigins()).containsExactlyInAnyOrder("nav.no", "oera.no");
        System.setProperty(CORS_ALLOWED_ORIGINS, " nav.no, oera.no ");
        assertThat(getAllowedOrigins()).containsExactlyInAnyOrder("nav.no", "oera.no");
    }

}