package no.nav.apiapp.util;

import org.junit.Test;

import static no.nav.apiapp.util.UrlUtils.sluttMedSlash;
import static org.assertj.core.api.Assertions.assertThat;


public class UrlUtilsTest {

    @Test
    public void sluttMedSlash_() {
        assertThat(sluttMedSlash("")).isEqualTo("/");
        assertThat(sluttMedSlash("/")).isEqualTo("/");
        assertThat(sluttMedSlash("/abc")).isEqualTo("/abc/");
    }

}