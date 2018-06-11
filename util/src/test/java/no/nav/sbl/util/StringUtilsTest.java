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

    @Test
    public void substring_from() {
        assertThat(StringUtils.substring(null, 5)).isEqualTo("");
        assertThat(StringUtils.substring("", 5)).isEqualTo("");
        assertThat(StringUtils.substring("abc", 5)).isEqualTo("");
        assertThat(StringUtils.substring("abc", 1)).isEqualTo("bc");
        assertThat(StringUtils.substring("abc", 0)).isEqualTo("abc");
        assertThat(StringUtils.substring("abc", -10)).isEqualTo("abc");
    }

    @Test
    public void substring_from_to() {
        assertThat(StringUtils.substring(null, 5, 10)).isEqualTo("");
        assertThat(StringUtils.substring("", 5, 1)).isEqualTo("");
        assertThat(StringUtils.substring("abc", 5, 10)).isEqualTo("");
        assertThat(StringUtils.substring("abc", 1,3)).isEqualTo("bc");
        assertThat(StringUtils.substring("abc", 1,10)).isEqualTo("bc");
        assertThat(StringUtils.substring("abc", 0)).isEqualTo("abc");
        assertThat(StringUtils.substring("abc", -10,50)).isEqualTo("abc");
        assertThat(StringUtils.substring("abc", -10,-50)).isEqualTo("");
    }

}