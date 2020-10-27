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
     * Performs a raw graphql request to PDL.
     * @param gqlRequestJson the json will be forwarded to PDL unchanged as the body of the request
     * @return the raw json from the body of the PDL response
     */
    String rawRequest(String gqlRequestJson);

    /**
     * Performs a graphql request to PDL
     * @param graphqlRequest request to be sent to PDL, will be serialized directly to JSON
     * @param graphqlResponseClass response from PDL, will de deserialized directly from JSON
     * @param <D> data type which is used to specify the "data" field inside the graphql response
     * @return parsed graphql response with the data type specified
     */
    <D extends GraphqlResponse> D request(GraphqlRequest<?> graphqlRequest, Class<D> graphqlResponseClass);

}
