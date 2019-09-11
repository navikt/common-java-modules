package no.nav.pact.runner;

import au.com.dius.pact.provider.junit.PactRunner;
import no.nav.fasit.FasitUtils;
import no.nav.fasit.TestUser;
import no.nav.fasit.dto.RestService;
import no.nav.testconfig.security.ISSOProvider;
import no.nav.sbl.dialogarena.test.ssl.SSLTestUtils;

import java.net.HttpCookie;
import java.util.List;

import static javax.ws.rs.core.UriBuilder.fromUri;
import static no.nav.util.sbl.EnvironmentUtils.Type.PUBLIC;
import static no.nav.util.sbl.EnvironmentUtils.getOptionalProperty;
import static no.nav.util.sbl.EnvironmentUtils.setProperty;

public class NavPactRunner extends PactRunner {

    public static final String PACT_USER_FASIT_ALIAS = "pactuser";

    static {
        SSLTestUtils.disableCertificateChecks();

        // optional slik at verdier kan overstyres via lokalt
        if (!getOptionalProperty("PACT_BROKER", "PACT_USERNAME", "PACT_PASSWORD").isPresent()) {
            RestService pactBrokerUrl = FasitUtils.getRestService("pactbroker");
            String baseUrl = pactBrokerUrl.getUrl();
            TestUser pactUser = FasitUtils.getTestUser(PACT_USER_FASIT_ALIAS);

            // Dette gjør at pact-annotasjonene til NavHttpsPactTest og NavHttpPactTest får de rette verdiene
            setProperty("PACT_BROKER", fromUri(baseUrl).build().getHost(), PUBLIC);
            setProperty("PACT_USERNAME", pactUser.username, PUBLIC);
            setProperty("PACT_PASSWORD", pactUser.password, PUBLIC);
        }
    }

    private static List<HttpCookie> issoCookies = ISSOProvider.getISSOCookies();

    public NavPactRunner(Class<?> clazz) {
        super(clazz);
    }

}
