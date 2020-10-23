package no.nav.common.client.pdl;

import no.nav.common.client.utils.graphql.GraphqlRequest;
import no.nav.common.client.utils.graphql.GraphqlResponse;
import no.nav.common.health.HealthCheck;

/**
 * Generic client for sending requests to PDL.
 * More information can be found at https://navikt.github.io/pdl/index-intern.html
 */
public interface PdlClient extends HealthCheck {

    /**
     * Performs a raw Graphql request to PDL.
     * @param gqlRequestJson the json will be forwarded to PDL unchanged as the body of the request
     * @return the raw json from the body of the PDL response
     */
    String rawRequest(String gqlRequestJson);

    <D> GraphqlResponse<D> request(GraphqlRequest<?> graphqlRequest, Class<D> dataClass);

}
