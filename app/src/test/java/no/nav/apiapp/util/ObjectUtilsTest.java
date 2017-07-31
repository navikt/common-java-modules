package no.nav.apiapp.util;

import org.junit.Test;

import static no.nav.apiapp.util.ObjectUtils.isEqual;
import static no.nav.apiapp.util.ObjectUtils.notEqual;
import static org.assertj.core.api.Assertions.assertThat;


public class ObjectUtilsTest {

    @Test
    public void isEqual_() {
        assertThat(isEqual("", "")).isTrue();
        assertThat(isEqual(null, null)).isTrue();

        assertThat(isEqual("a", "b")).isFalse();
        assertThat(isEqual(null, "b")).isFalse();
        assertThat(isEqual("a", null)).isFalse();
    }

    @Test
    public void notEqual_() {
        assertThat(notEqual("a", "b")).isTrue();
    }

}