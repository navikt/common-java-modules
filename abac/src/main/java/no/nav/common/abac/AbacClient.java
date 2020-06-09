package no.nav.common.abac;

import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.health.HealthCheck;

public interface AbacClient extends HealthCheck {

    String sendRawRequest(String xacmlRequestJson);

    XacmlResponse sendRequest(XacmlRequest xacmlRequest);

}
