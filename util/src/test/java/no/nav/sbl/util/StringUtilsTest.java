package no.nav.sbl.util;

import org.junit.Test;

import static no.nav.sbl.util.StringUtils.notNullOrEmpty;
import static no.nav.sbl.util.StringUtils.nullOrEmpty;
import static org.assertj.core.api.Assertions.assertThat;

public class StringUtilsTest {

    @Test
    public void notNullOrEmpty_(){
        assertThat(notNullOrEmpty(null)).isFalse();
        assertThat(notNullOrEmpty("")).isFalse();
        assertThat(notNullOrEmpty(" ")).isFalse();

        assertThat(notNullOrEmpty("a")).isTrue();
    }

    @Test
    public void nullOrEmpty_(){
        assertThat(nullOrEmpty(null)).isTrue();
        assertThat(nullOrEmpty("")).isTrue();
        assertThat(nullOrEmpty(" ")).isTrue();

        assertThat(nullOrEmpty("a")).isFalse();
    }

    @Test
    public void toString_(){
        assertThat(StringUtils.toString(null)).isEqualTo("");
        assertThat(StringUtils.toString("a")).isEqualTo("a");
    }

}