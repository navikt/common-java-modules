package no.nav.common.token_client.client;

/**
 * A token client which performs the client credentials OAuth 2.0 flow.
 * {@code MachineToMachineTokenClient} is used to create tokens used for communication between machines (applications) without user interaction.
 * See: https://datatracker.ietf.org/doc/html/rfc6749#section-1.3.4
 */
public interface MachineToMachineTokenClient {

    /**
     * Creates a token used for machine to machine communication.
     * @param tokenScope the scope/id of the machine (application) that will receive the token
     * @return JWT access token
     */
    String createMachineToMachineToken(String tokenScope);

}
