package no.nav.common.test;

import no.nav.common.test.SystemProperties.NotFound;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;

import static java.lang.System.getProperty;
import static no.nav.common.test.SystemProperties.setTemporaryProperty;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;


public class SystemPropertiesTest {

    public static final String TEST_PROP = "test-prop";

    @Test
    public void setsProperties() {
        SystemProperties.setFrom("dummy.properties");
        assertThat(getProperty("prop01"), not(nullValue()));
    }

    @Test(expected = NotFound.class)
    public void throwsExceptionWhenNotFindingResource() {
        SystemProperties.setFrom("does-not-exist.properties");
    }

    @Test
    public void overwritesProperties() {
        System.setProperty("vatske", "busy");
        SystemProperties.setFrom(new ByteArrayInputStream("vatske=busy busy".getBytes()));
        assertThat(getProperty("vatske"), is("busy busy"));
    }

    @Test
    public void setTemporaryProperty_propertyScopedToCallback_propagatesExceptions_restoresOriginalValue() {
        System.setProperty(TEST_PROP, "original-value");
        assertThat(getProperty(TEST_PROP), equalTo("original-value"));

        Runnable a = Mockito.mock(Runnable.class);
        Runnable b = Mockito.mock(Runnable.class);
        Runnable c = Mockito.mock(Runnable.class);

        setTemporaryProperty(TEST_PROP, "a", () -> {
            a.run();
            assertThat(getProperty(TEST_PROP), equalTo("a"));

            setTemporaryProperty(TEST_PROP, "b", () -> {
                b.run();
                assertThat(getProperty(TEST_PROP), equalTo("b"));
            });

            assertThatThrownBy(() -> {
                        setTemporaryProperty(TEST_PROP, "c", () -> {
                            c.run();
                            assertThat(getProperty(TEST_PROP), equalTo("c"));
                            throw new TestException();
                        });
                    }
            ).isInstanceOf(TestException.class);

            assertThat(getProperty(TEST_PROP), equalTo("a"));
        });

        assertThat(getProperty(TEST_PROP), equalTo("original-value"));
        verify(a).run();
        verify(b).run();
        verify(c).run();
    }

    @Test
    public void setTemporaryProperty_handle_null_values() {
        System.clearProperty(TEST_PROP);
        assertThat(getProperty(TEST_PROP), nullValue());
        setTemporaryProperty(TEST_PROP, null, () -> {
            //noop
        });
        assertThat(getProperty(TEST_PROP), nullValue());
    }

}
