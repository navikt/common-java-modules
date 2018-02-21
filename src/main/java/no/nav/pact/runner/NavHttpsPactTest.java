package no.nav.pact.runner;

import au.com.dius.pact.provider.junit.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junit.TargetRequestFilter;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactBrokerAuth;
import no.nav.dialogarena.config.security.ISSOProvider;
import org.apache.http.HttpRequest;
import org.slf4j.Logger;

import javax.ws.rs.core.HttpHeaders;
import java.net.HttpCookie;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@PactBroker(
        protocol = "https",
        host = "${PACT_BROKER}",
        authentication = @PactBrokerAuth(username = "${PACT_USERNAME}", password = "${PACT_PASSWORD}"),
        port = "443", tags = "${PACT_TAGS:latest}")
@IgnoreNoPactsToVerify
public abstract class NavHttpsPactTest extends PactHttpTarget {

    private static final Logger LOG = getLogger(NavHttpsPactTest.class);

    private static List<HttpCookie> issoCookies = ISSOProvider.getISSOCookies();

    private boolean useIssoRequestFilter;

    public NavHttpsPactTest() {
        this(true);
    }

    public NavHttpsPactTest(boolean useIssoRequestFilter) {
        this.useIssoRequestFilter = useIssoRequestFilter;
    }

    @TargetRequestFilter
    public void requestFilter(HttpRequest httpRequest) {
        if (useIssoRequestFilter) {
            LOG.info("Setting ISSO cookies for " + httpRequest.getRequestLine().getUri());
            issoCookies.forEach(c -> {
                httpRequest.addHeader(HttpHeaders.COOKIE, String.format("%s=%s", c.getName(), c.getValue()));
            });
        }
    }
}
