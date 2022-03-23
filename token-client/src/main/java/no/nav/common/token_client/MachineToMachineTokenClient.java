package no.nav.common.token_client;

import com.nimbusds.oauth2.sdk.token.AccessToken;

public interface MachineToMachineTokenClient {

    AccessToken createToken(String tokenScope);

}
