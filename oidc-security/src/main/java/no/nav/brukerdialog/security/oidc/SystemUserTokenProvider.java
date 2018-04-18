package no.nav.brukerdialog.security.oidc;


import no.nav.brukerdialog.security.domain.IdToken;
import no.nav.brukerdialog.security.domain.IdTokenAndRefreshToken;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.oidc.provider.IssoOidcProvider;
import no.nav.sbl.rest.RestUtils;

import javax.ws.rs.client.Client;
import java.time.Instant;

import static no.nav.brukerdialog.security.Constants.*;

import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_PASSWORD;
import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_USERNAME;
import static no.nav.brukerdialog.tools.Utils.getSystemProperty;

public class SystemUserTokenProvider {
    public final String openAmHost = getIssoHostUrl();
    private final String openamClientUsername = getIssoRpUserUsername();
    private final String oidcRedirectUrl = getOidcRedirectUrl();
    private final String srvUsername = getSystemProperty(SYSTEMUSER_USERNAME);
    private final String srvPassword = getSystemProperty(SYSTEMUSER_PASSWORD);
    private final String authenticateUri = "json/authenticate?authIndexType=service&authIndexValue=adminconsoleservice";

    private final IdTokenAndRefreshTokenProvider idTokenAndRefreshTokenProvider = new IdTokenAndRefreshTokenProvider();
    private final Client client = RestUtils.createClient();
    private final OidcTokenValidator validator = new OidcTokenValidator();
    private final IssoOidcProvider oidcProvider = new IssoOidcProvider();

    private IdToken idToken;

    public SystemUserTokenProvider() {
    }

    public String getToken() {
        if(tokenIsSoonExpired()) {
            refreshToken();
        }
        return idToken.getIdToken().getToken();
    }

    private void refreshToken() {
        String openAmSessionToken = OpenAmUtils.getSessionToken(srvUsername, srvPassword, konstruerFullstendingAuthUri(openAmHost, authenticateUri), client);
        String authorizationCode = OpenAmUtils.getAuthorizationCode(openAmHost, openAmSessionToken, openamClientUsername, oidcRedirectUrl, client);
        IdTokenAndRefreshToken idTokenAndRefreshToken = idTokenAndRefreshTokenProvider.getToken(authorizationCode, oidcRedirectUrl);
        OidcCredential idToken = idTokenAndRefreshToken.getIdToken();
        String jwtToken = idToken.getToken();
        OidcTokenValidatorResult validationResult = validator.validate(jwtToken, oidcProvider);

        if (validationResult.isValid()) {
            this.idToken = new IdToken(idToken, validationResult.getExpSeconds());
        } else {
            throw new OidcTokenException("Kunne ikke validere token: "+validationResult.getErrorMessage());
        }
    }

    public static String konstruerFullstendingAuthUri(String openAmHost, String authUri ) {
        return openAmHost.replace("oauth2",authUri);
    }

    private boolean tokenIsSoonExpired() {
        return idToken == null || idToken.getExpirationTimeSeconds() * 1000 - Instant.now().toEpochMilli() < getMinimumTimeToExpireBeforeRefresh();
    }

    private int getMinimumTimeToExpireBeforeRefresh() {
        return Integer.parseInt(System.getProperty(REFRESH_TIME, "60")) * 1000;
    }
}
