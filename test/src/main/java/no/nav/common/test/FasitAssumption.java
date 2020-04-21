package no.nav.common.test;

import java.io.IOException;
import java.net.InetAddress;

import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

public class FasitAssumption {

    public static void assumeFasitAccessible() {
        try {
            assumeTrue(InetAddress.getByName("fasit.adeo.no").isReachable(5000));
        } catch (IOException e) {
            assumeNoException(e);
        }
    }

}
