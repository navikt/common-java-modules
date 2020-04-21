package no.nav.common.rest.client;


import no.nav.common.rest.RestUtils;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import static javax.ws.rs.core.HttpHeaders.COOKIE;

public class PipingRestClient {

    private final Provider<HttpServletRequest> httpServletRequestProvider;
    private final String basePath;

    public PipingRestClient(Provider<HttpServletRequest> httpServletRequestProvider, String basePath) {
        if (basePath == null || basePath.trim().length() == 0) {
            throw new IllegalArgumentException("mangler basePath");
        }
        if (httpServletRequestProvider == null) {
            throw new IllegalArgumentException("mangler httpServletRequestProvider");
        }
        this.httpServletRequestProvider = httpServletRequestProvider;
        this.basePath = basePath;
    }

    public RestRequest request(String relativePath) {
        Client client = RestUtils.createClient();
        WebTarget webTarget = client.target(basePath + relativePath);
        return new RestRequest(this::pipeCookies, webTarget);
    }

    private Invocation.Builder pipeCookies(Invocation.Builder request) {
        HttpServletRequest httpServletRequest = httpServletRequestProvider.get();
        return request.header(COOKIE, httpServletRequest.getHeader(COOKIE));
    }

}
