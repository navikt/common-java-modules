package no.nav.sbl.util;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class ExceptionUtilsTest {

    @Test(expected = IOException.class)
    public void throwUnchecked() {
        ExceptionUtils.throwUnchecked(new IOException("checked"));
    }

}