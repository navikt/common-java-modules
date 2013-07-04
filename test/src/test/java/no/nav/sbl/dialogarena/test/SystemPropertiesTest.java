package no.nav.sbl.dialogarena.test;

import no.nav.sbl.dialogarena.test.SystemProperties.NotFound;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;


public class SystemPropertiesTest {

    @Test
    public void setsProperties() {
        SystemProperties.setFrom("dummy.properties");
        assertThat(System.getProperty("prop01"), not(nullValue()));
    }

    @Test(expected = NotFound.class)
    public void throwsExceptionWhenNotFindingResource() {
        SystemProperties.setFrom("does-not-exist.properties");
    }

    @Test
    public void overwritesProperties() {
        System.setProperty("vatske", "busy");
        SystemProperties.setFrom(new ByteArrayInputStream("vatske=busy busy".getBytes()));
        assertThat(System.getProperty("vatske"), is("busy busy"));
    }
}
