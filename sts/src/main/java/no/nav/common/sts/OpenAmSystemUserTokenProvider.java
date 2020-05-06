package no.nav.common.sts;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import lombok.SneakyThrows;
import no.nav.common.auth.oidc.discovery.OidcDiscoveryConfiguration;
import no.nav.common.auth.oidc.discovery.OidcDiscoveryConfigurationClient;
import no.nav.common.rest.client.RestClient;
import okhttp3.OkHttpClient;

import static no.nav.common.sts.SystemUserTokenUtils.tokenNeedsRefresh;

/**
 * Retrieves system user tokens through OpenAM
 * Tokens from OpenAM are considered deprecated, use tokens from the NAIS STS instead
 */
public class OpenAmSystemUserTokenProvider implements SystemUserTokenProvider {

    private final OkHttpClient client;

    private final String redirectUrl;

    private final String authorizationUrl;

    private final String tokenUrl;

    private final String issoRpUserUsername;

    private final String issoRpUserPassword;

    private JWT accessToken;

    /*
        Example values:
        discoveryUrl = https://isso-q.adeo.no/isso/oauth2/.well-known/openid-configuration
        redirectUrl = https://app-q0.adeo.no/veilarblogin/api/login
        issoRpUserUsername = veilarblogin-q0
        issoRpUserPassword = <secret>
    */
    public OpenAmSystemUserTokenProvider(String discoveryUrl, String redirectUrl, String issoRpUserUsername, String issoRpUserPassword) {
        OidcDiscoveryConfigurationClient client = new OidcDiscoveryConfigurationClient();
        OidcDiscoveryConfiguration configuration = client.fetchDiscoveryConfiguration(discoveryUrl);

        this.redirectUrl = redirectUrl;

        this.authorizationUrl = configuration.authorizationEndpoint;
        this.tokenUrl = configuration.tokenEndpoint;

        this.issoRpUserUsername = issoRpUserUsername;
        this.issoRpUserPassword = issoRpUserPassword;
        this.client = RestClient.baseClient();
    }

    public OpenAmSystemUserTokenProvider(
            String tokenUrl, String authorizationUrl, String redirectUrl,
            String issoRpUserUsername, String issoRpUserPassword, OkHttpClient client
    ) {
        this.tokenUrl = tokenUrl;
        this.authorizationUrl = authorizationUrl;
        this.redirectUrl = redirectUrl;
        this.issoRpUserUsername = issoRpUserUsername;
        this.issoRpUserPassword = issoRpUserPassword;
        this.client = client;
    }

    @Override
    public String getSystemUserToken() {
        if(tokenNeedsRefresh(accessToken)) {
            accessToken = fetchSystemUserToken();
        }

        return accessToken.getParsedString();
    }

    @SneakyThrows
    private JWT fetchSystemUserToken() {
        String openAmSessionToken = OpenAmUtils.getSessionToken(issoRpUserUsername, issoRpUserPassword, authorizationUrl, client);
        String authorizationCode = OpenAmUtils.getAuthorizationCode(authorizationUrl, openAmSessionToken, issoRpUserUsername, redirectUrl, client);
        String token = OpenAmUtils.exchangeCodeForToken(authorizationCode, tokenUrl, redirectUrl, issoRpUserUsername, issoRpUserPassword, client);

        return JWTParser.parse(token);
    }

}
