package no.nav.apiapp.util;

import org.junit.Test;

import static no.nav.apiapp.util.ObjectUtils.*;
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

    @Test
    public void min_() {
        assertThat(min(1, 2)).isEqualTo(1);
        assertThat(min(2, 1)).isEqualTo(1);
        assertThat(min("b", "a")).isEqualTo("a");
        assertThat(min("a", "b")).isEqualTo("a");
        assertThat(min(null, "a")).isEqualTo("a");
        assertThat(min("a", null)).isEqualTo("a");
        assertThat(min((String)null, null)).isNull();
    }

    @Test
    public void max_() {
        assertThat(max(1, 2)).isEqualTo(2);
        assertThat(max(2, 1)).isEqualTo(2);
        assertThat(max("b", "a")).isEqualTo("b");
        assertThat(max("a", "b")).isEqualTo("b");
        assertThat(max(null, "a")).isEqualTo("a");
        assertThat(max("a", null)).isEqualTo("a");
        assertThat(max((String)null, null)).isNull();
    }

}
