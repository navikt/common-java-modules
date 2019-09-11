package no.nav.sbl.util;

import org.junit.Test;

import static no.nav.util.sbl.AssertUtils.assertNotNull;
import static no.nav.util.sbl.AssertUtils.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


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
