package no.nav.apiapp.util;

import org.junit.Test;

import static no.nav.apiapp.util.UrlUtils.sluttMedSlash;
import static no.nav.apiapp.util.UrlUtils.startMedSlash;
import static org.assertj.core.api.Assertions.assertThat;


public class UrlUtilsTest {

    @Test
    public void sluttMedSlash_() {
        assertThat(sluttMedSlash(null)).isEqualTo("/");
        assertThat(sluttMedSlash("")).isEqualTo("/");
        assertThat(sluttMedSlash("/")).isEqualTo("/");
        assertThat(sluttMedSlash("/abc")).isEqualTo("/abc/");
    }

    @Test
    public void startMedSlash_() {
        assertThat(startMedSlash(null)).isEqualTo("/");
        assertThat(startMedSlash("")).isEqualTo("/");
        assertThat(startMedSlash("abc")).isEqualTo("/abc");
        assertThat(startMedSlash("/abc")).isEqualTo("/abc");
    }

}