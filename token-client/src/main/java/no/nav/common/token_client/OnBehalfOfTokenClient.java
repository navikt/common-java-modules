package no.nav.common.token_client;

public interface OnBehalfOfTokenClient {

    String exchangeOnBehalfOfToken(String tokenScope, String accessToken);

}
