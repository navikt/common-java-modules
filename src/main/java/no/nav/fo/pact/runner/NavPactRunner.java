package no.nav.fo.pact.runner;

import au.com.dius.pact.provider.junit.PactRunner;
import no.nav.sbl.dialogarena.test.ssl.SSLTestUtils;

public class NavPactRunner extends PactRunner {

    static {
        SSLTestUtils.disableCertificateChecks();
    }

    public NavPactRunner(Class<?> clazz) {
        super(clazz);
    }

}