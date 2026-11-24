package no.nav.common.utils;

import static no.nav.common.utils.AssertUtils.assertNotNull;
import static no.nav.common.utils.AssertUtils.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class AssertUtilsTest {

    @Test
    public void assertNotNull_() {
        assertThat(assertNotNull("test")).isEqualTo("test");
        assertThatThrownBy(() -> assertNotNull(null)).isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    public void assertTrue_() {
        assertThat(assertTrue(true)).isTrue();
        assertThatThrownBy(() -> assertTrue(false)).isExactlyInstanceOf(IllegalStateException.class);
    }
}
