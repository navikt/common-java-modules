package no.nav.common.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ExceptionUtilsTest {

    @Test
    public void throwUnchecked() {
        assertThrows(IOException.class, () -> ExceptionUtils.throwUnchecked(new IOException("checked")));
    }

    @Test
    public void getRootCause() {
        assertThat(ExceptionUtils.getRootCause(new RuntimeException(new IllegalStateException(new IOException()))))
                .isExactlyInstanceOf(IOException.class);
    }
}
