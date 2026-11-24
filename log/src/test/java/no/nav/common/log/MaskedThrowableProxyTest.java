package no.nav.common.log;

import static ch.qos.logback.classic.spi.ThrowableProxyUtil.asString;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class MaskedThrowableProxyTest {

    private static final String FNR = "12345678901";

    @Test
    public void smokeTest() {
        RuntimeException sensitiveException =
                new RuntimeException(FNR, new IllegalArgumentException(FNR, new Exception(FNR)));
        sensitiveException.addSuppressed(new IllegalStateException(FNR, new IOException(FNR)));

        ThrowableProxy sensitiveThrowableProxy = new ThrowableProxy(sensitiveException);
        IThrowableProxy maskedThrowableProxy = MaskedThrowableProxy.mask(sensitiveThrowableProxy);

        assertThat(asString(sensitiveThrowableProxy)).contains(FNR);
        assertThat(asString(maskedThrowableProxy)).doesNotContain(FNR);
    }
}
