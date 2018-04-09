package no.nav.apiapp.rest;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static no.nav.apiapp.rest.NavCorsFilter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class NavCorsFilterTest {

    @Test
    public void validOrigin_() {
        assertInvalidOrigin("origin", null);
        assertInvalidOrigin("", null);
        assertInvalidOrigin(null, null, null);
        assertInvalidOrigin("abcd.nav.no");
        assertInvalidOrigin("evil.com", ".nav.no");
        assertInvalidOrigin("evil.com", "", null);
        assertInvalidOrigin("abcd.nav.no", ".nav.noo");

        assertValidOrigin("abcd.nav.no", ".nav.no");
        assertValidOrigin("abcd.nav.no", ".nav.no");
        assertValidOrigin("abcd.nav.no", ".oera.no", ".nav.no");
        assertValidOrigin("abcd.nav.no", ".oera.no", "", null, ".nav.no");
    }

    private void assertValidOrigin(String origin, String... validSubDomains) {
        assertThat(validOrigin(origin, Arrays.asList(validSubDomains))).isTrue();
    }

    private void assertInvalidOrigin(String origin, String... validSubDomains) {
        assertThat(validOrigin(origin, validSubDomains != null ? Arrays.asList(validSubDomains) : Collections.emptyList())).isFalse();
    }

    @Test
    public void hentKommaseparertListe() {
        System.clearProperty(CORS_ALLOWED_ORIGINS);
        assertThat(getAllowedOrigins()).isEmpty();
        System.setProperty(CORS_ALLOWED_ORIGINS, "");
        assertThat(getAllowedOrigins()).isEmpty();
        System.setProperty(CORS_ALLOWED_ORIGINS, ".nav.no,.oera.no");
        assertThat(getAllowedOrigins()).containsExactlyInAnyOrder(".nav.no", ".oera.no");
        System.setProperty(CORS_ALLOWED_ORIGINS, " .nav.no, .oera.no ");
        assertThat(getAllowedOrigins()).containsExactlyInAnyOrder(".nav.no", ".oera.no");
    }

    @Test
    public void krevSubdomene() {
        System.setProperty(CORS_ALLOWED_ORIGINS, "ikke.subdomene.no");
        assertThatThrownBy(NavCorsFilter::getAllowedOrigins).isInstanceOf(IllegalArgumentException.class);
    }

}