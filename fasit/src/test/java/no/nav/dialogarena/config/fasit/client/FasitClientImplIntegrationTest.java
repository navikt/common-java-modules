package no.nav.dialogarena.config.fasit.client;

import no.nav.sbl.dialogarena.test.FasitAssumption;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.net.ssl.SSLException;
import java.util.concurrent.atomic.AtomicBoolean;

public class FasitClientImplIntegrationTest {

    @BeforeClass
    public static void test() {
        FasitAssumption.assumeFasitAccessible();
    }

    @Test
    public void httpClient() {
        AtomicBoolean hasFailed = new AtomicBoolean();
        FasitClientImpl.httpClient((c) -> {
            if (hasFailed.get()) {
                return "ok!";
            } else {
                hasFailed.set(true);
                throw new SSLException("handshake_failure");
            }
        });
    }

}