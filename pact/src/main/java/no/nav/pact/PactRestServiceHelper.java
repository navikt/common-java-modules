package no.nav.pact;

import no.nav.testconfig.security.ISSOProvider;
import no.nav.sbl.rest.RestUtils;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import java.util.Base64;

/**
 * Use this class to build a HTTP request with wither ISSO or Basic Auth authentication.
 *
 * See PactRestServiceHelperTest for example usage.
 */
public class PactRestServiceHelper {

    private String target;

    private Invocation.Builder request;

    public PactRestServiceHelper() {}

    /**
     * Constructs a new helper
     * @param target A fully qualified HTTP Url
     */
    public PactRestServiceHelper(String target) {
        this.target = target;
    }

    /**
     * The URL to request.
     *
     * @param target A fully qualified HTTP Url
     * @return This helper
     */
    public PactRestServiceHelper withTarget(String target) {
        this.target = target;
        return this;
    }

    /**
     * Adds ISSO cookies to the request.
     * @return This helper
     */
    public PactRestServiceHelper withISSO() {
        request = RestUtils.createClient().target(target).request();
        ISSOProvider.getISSOCookies().forEach(c -> {
            request.cookie(c.getName(), c.getValue());
        });
        return this;
    }

    /**
     * Adds basic auth to the request.
     * @param username The username to use.
     * @param password The password to use.
     * @return This helper
     */
    public PactRestServiceHelper withBasicAuth(String username, String password) {
        request = RestUtils.createClient().target(target).request();
        request.header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
        return this;
    }

    /**
     * Return a copy of JAX-RS request builder.
     */
    public Invocation.Builder getRequest() {
        if (request == null) request = RestUtils.createClient().target(target).request();
        return request;
    }

    /**
     * Check if the target is alive and responding.
     * @return True, if and only if target is alive.
     */
    public Boolean isAlive() {
        return getRequest().get().getStatus() == 200;
    }
}
