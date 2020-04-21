package no.nav.common.log;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.Test;

import java.io.IOException;

import static ch.qos.logback.classic.spi.ThrowableProxyUtil.asString;
import static org.assertj.core.api.Assertions.assertThat;

public class MaskedThrowableProxyTest {

    private static final String FNR = "12345678901";

    @Test
    public void smoketest() {
        RuntimeException sensitiveException = new RuntimeException(FNR, new IllegalArgumentException(FNR, new Exception(FNR)));
        sensitiveException.addSuppressed(new IllegalStateException(FNR, new IOException(FNR)));
        sensitiveException.printStackTrace();

        ThrowableProxy sensitiveThrowableProxy = new ThrowableProxy(sensitiveException);
        IThrowableProxy maskedThrowableProxy = MaskedThrowableProxy.mask(sensitiveThrowableProxy);

        assertThat(asString(sensitiveThrowableProxy)).contains(FNR);
        assertThat(asString(maskedThrowableProxy)).doesNotContain(FNR);
    }

}