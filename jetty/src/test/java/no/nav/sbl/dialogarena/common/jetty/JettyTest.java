package no.nav.sbl.dialogarena.common.jetty;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JettyTest {

    @Test
    public void classpathPattern__matchJarsAndClassDirectoriesOnly() throws Exception {
        assertThat("/no-match").doesNotMatch(Jetty.CLASSPATH_PATTERN);
        assertThat("/a/b/c/libatk-wrapper.so").doesNotMatch(Jetty.CLASSPATH_PATTERN);

        assertThat("/a/b/c/some.jar").matches(Jetty.CLASSPATH_PATTERN);
        assertThat("/a/b/c/classes").matches(Jetty.CLASSPATH_PATTERN);
        assertThat("/a/b/c/classes/").matches(Jetty.CLASSPATH_PATTERN);
        assertThat("/a/b/c/test-classes").matches(Jetty.CLASSPATH_PATTERN);
        assertThat("/a/b/c/test-classes/").matches(Jetty.CLASSPATH_PATTERN);
    }

}
