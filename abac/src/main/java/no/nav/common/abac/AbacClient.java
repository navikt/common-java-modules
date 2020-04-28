package no.nav.common.abac;

import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.XacmlResponse;

public interface AbacClient {

    String sendRawRequest(String xacmlRequestJson);

    XacmlResponse sendRequest(XacmlRequest xacmlRequest);

}
