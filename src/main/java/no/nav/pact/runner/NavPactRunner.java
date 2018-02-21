package no.nav.pact.runner;

import au.com.dius.pact.provider.junit.PactRunner;
import no.nav.dialogarena.config.security.ISSOProvider;
import no.nav.sbl.dialogarena.test.ssl.SSLTestUtils;

import java.net.HttpCookie;
import java.util.List;

public class NavPactRunner extends PactRunner {

    static {
        SSLTestUtils.disableCertificateChecks();
    }

    private static List<HttpCookie> issoCookies = ISSOProvider.getISSOCookies();

    public NavPactRunner(Class<?> clazz) {
        super(clazz);
    }

}