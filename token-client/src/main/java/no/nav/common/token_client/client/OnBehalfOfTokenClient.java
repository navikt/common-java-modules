package no.nav.common.token_client.client;

/**
 * A token client which performs the on-behalf-of (token exchange) OAuth 2.0 flow.
 * {@code OnBehalfOfTokenClient} is used to create tokens for communication between machines (applications) where the
 * user's context must be preserved.
 * See: https://datatracker.ietf.org/doc/html/rfc8693
 */
public interface OnBehalfOfTokenClient {

    /**
     * Exchanges a JWT access token for a new token scoped to a specific application.
     * @param tokenScope the scope/id of the application that will receive the token
     * @param accessToken the JWT access token that will be exchanged for a new token
     * @return JWT access token
     */
    String exchangeOnBehalfOfToken(String tokenScope, String accessToken);

}
