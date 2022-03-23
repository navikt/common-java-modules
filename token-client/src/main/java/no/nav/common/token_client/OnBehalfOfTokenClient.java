package no.nav.common.token_client;

import com.nimbusds.oauth2.sdk.token.AccessToken;

public interface OnBehalfOfTokenClient {

    AccessToken exchangeToken(String tokenScope, String accessToken);

}
