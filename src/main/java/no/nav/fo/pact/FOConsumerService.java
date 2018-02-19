package no.nav.fo.pact;

import no.nav.dialogarena.config.security.ISSOProvider;
import no.nav.sbl.rest.RestUtils;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import java.util.Base64;

public class FOConsumerService {

    private String target;

    private Invocation.Builder request;

    public FOConsumerService() {}

    public FOConsumerService(String target) {
        this.target = target;
    }

    public FOConsumerService withTarget(String target) {
        this.target = target;
        return this;
    }

    public FOConsumerService withISSO() {
        request = RestUtils.createClient().target(target).request();
        ISSOProvider.getISSOCookies().forEach(c -> {
            request.cookie(c.getName(), c.getValue());
        });
        return this;
    }

    public FOConsumerService withBasicAuth(String username, String password) {
        request = RestUtils.createClient().target(target).request();
        request.header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
        return this;
    }

    public Boolean isAlive() {
        if (request == null) request = RestUtils.createClient().target(target).request();
        return request.get().getStatus() == 200;
    }

    public Invocation.Builder getRequest() {
        if (request == null) request = RestUtils.createClient().target(target).request();
        return request;
    }
}
