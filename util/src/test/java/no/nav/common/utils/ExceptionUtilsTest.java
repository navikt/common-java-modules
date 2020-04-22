package no.nav.common.utils;

import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


public class ExceptionUtilsTest {

    @Test(expected = IOException.class)
    public void throwUnchecked() {
        ExceptionUtils.throwUnchecked(new IOException("checked"));
    }

    @Test
    public void getRootCause() {
        assertThat(ExceptionUtils.getRootCause(new RuntimeException(new IllegalStateException(new IOException()))))
                .isExactlyInstanceOf(IOException.class);
    }

}