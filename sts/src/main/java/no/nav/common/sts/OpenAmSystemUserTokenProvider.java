package no.nav.common.sts;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import lombok.SneakyThrows;
import no.nav.common.auth.oidc.discovery.OidcDiscoveryConfiguration;
import no.nav.common.auth.oidc.discovery.OidcDiscoveryConfigurationClient;
import no.nav.common.rest.client.RestClient;
import no.nav.common.sts.utils.OpenAmUtils;
import no.nav.common.utils.Credentials;
import okhttp3.OkHttpClient;

import static no.nav.common.sts.utils.StsTokenUtils.tokenNeedsRefresh;

/**
 * Retrieves system user tokens through OpenAM
 * Tokens from OpenAM are considered deprecated, use tokens from the NAIS STS instead
 */
public class OpenAmSystemUserTokenProvider implements SystemUserTokenProvider {

    private final OkHttpClient client;

    private final String redirectUrl;

    private final String authorizationUrl;

    private final String tokenUrl;

    private final Credentials issoRpCredentials;

    private final Credentials systemUserCredentials;

    private JWT accessToken;

    /*
        Example values:
        discoveryUrl = https://isso-q.adeo.no/isso/oauth2/.well-known/openid-configuration
        redirectUrl = https://app-q0.adeo.no/veilarblogin/api/login
        issoRpCredentials = {
            username: veilarblogin-q0
            password: <secret>
        }
         systemUserCredentials = {
            username: srvveilarbdemo
            password: <secret>
        }
    */
    public OpenAmSystemUserTokenProvider(String discoveryUrl, String redirectUrl, Credentials issoRpCredentials, Credentials systemUserCredentials) {
        OidcDiscoveryConfigurationClient client = new OidcDiscoveryConfigurationClient();
        OidcDiscoveryConfiguration configuration = client.fetchDiscoveryConfiguration(discoveryUrl);

        this.redirectUrl = redirectUrl;

        this.authorizationUrl = configuration.authorizationEndpoint;
        this.tokenUrl = configuration.tokenEndpoint;

        this.issoRpCredentials = issoRpCredentials;
        this.systemUserCredentials = systemUserCredentials;
        this.client = RestClient.baseClient();
    }

    public OpenAmSystemUserTokenProvider(
            String tokenUrl, String authorizationUrl, String redirectUrl,
            Credentials issoRpCredentials, Credentials systemUserCredentials, OkHttpClient client
    ) {
        this.tokenUrl = tokenUrl;
        this.authorizationUrl = authorizationUrl;
        this.redirectUrl = redirectUrl;
        this.issoRpCredentials = issoRpCredentials;
        this.systemUserCredentials = systemUserCredentials;
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
        String openAmSessionToken = OpenAmUtils.getSessionToken(systemUserCredentials, authorizationUrl, client);
        String authorizationCode = OpenAmUtils.getAuthorizationCode(authorizationUrl, openAmSessionToken, issoRpCredentials.username, redirectUrl, client);
        String token = OpenAmUtils.exchangeCodeForToken(authorizationCode, tokenUrl, redirectUrl, issoRpCredentials, client);

        return JWTParser.parse(token);
    }

}
