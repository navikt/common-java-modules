package no.nav.common.test.ssl;

import org.junit.Test;

public class SSLTestUtilsTest {

    @Test
    public void disableCertificateChecks(){
        SSLTestUtils.disableCertificateChecks();
    }

}