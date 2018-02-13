package no.nav.fo.pact;

import au.com.dius.pact.provider.junit.*;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactBrokerAuth;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import com.google.common.net.HttpHeaders;
import no.nav.dialogarena.config.security.ISSOProvider;
import no.nav.fo.pact.runner.NavPactRunner;
import org.apache.http.HttpRequest;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@RunWith(NavPactRunner.class)
@Provider("veilarbperson")
@Consumer("veilarbpersonfs")
@PactBroker(
        protocol = "https",
        host = "${PACT_BROKER:pact-broker.nais.preprod.local}",
        authentication = @PactBrokerAuth(username = "${PACT_USERNAME}", password = "${PACT_PASSWORD}"),
        port = "443")
@IgnoreNoPactsToVerify
public class ProviderTest {

    private static final Logger LOG = getLogger(ProviderTest.class);

    private final static String TARGET_URL = Optional.ofNullable(System.getenv("PACT_TARGET_URL")).orElseThrow(() -> new RuntimeException("PACT_TARGET_URL environment variable is not set."));

    @TestTarget
    public static Target target;

    private static List<HttpCookie> issoCookies = ISSOProvider.getISSOCookies();

    @BeforeClass
    public static void setUp() throws MalformedURLException {
        target = new HttpTarget(new URL(TARGET_URL));
    }

    @TargetRequestFilter
    public void requestFilter(HttpRequest httpRequest) {
        LOG.info(httpRequest.getRequestLine().getUri());
        issoCookies.forEach(c -> {
            httpRequest.addHeader(HttpHeaders.COOKIE, String.format("%s=%s", c.getName(), c.getValue()));
        });
    }

    @State("has a single person without children")
    public void verifyProviderStateSinglePersonNoChildren() {
        System.out.println("Single person");
    }

    @State("does not have person")
    public void verifyProviderStateNoData() {
        System.out.println("No data");
    }

    @State("is alive state")
    public void verifyIsAlive() {
        System.out.println("Is Alive");
    }
}