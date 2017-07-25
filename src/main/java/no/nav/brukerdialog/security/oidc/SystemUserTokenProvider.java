package no.nav.brukerdialog.security.oidc;


import no.nav.brukerdialog.security.domain.IdToken;
import no.nav.brukerdialog.security.domain.IdTokenAndRefreshToken;
import no.nav.brukerdialog.security.domain.OidcCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import static no.nav.brukerdialog.security.Constants.*;
import static no.nav.brukerdialog.security.oidc.OidcTokenValidator.OidcTokenValidatorResult;
import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_PASSWORD;
import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_USERNAME;
import static no.nav.brukerdialog.tools.Utils.getSystemProperty;

public class SystemUserTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(SystemUserTokenProvider.class);


    private final String openAmHost = getSystemProperty(ISSO_HOST_URL_PROPERTY_NAME);
    private final String openamClientUsername = getSystemProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME);
    private final String oidcRedirectUrl = getSystemProperty(OIDC_REDIRECT_URL);
    private final String srvUsername = getSystemProperty(SYSTEMUSER_USERNAME);
    private final String srvPassword = getSystemProperty(SYSTEMUSER_PASSWORD);
    private final String authenticateUri = "json/authenticate?authIndexType=service&authIndexValue=adminconsoleservice";

    private IdToken idToken;
    private IdTokenAndRefreshTokenProvider idTokenAndRefreshTokenProvider;

    private OidcTokenValidator validator;

    public SystemUserTokenProvider() {
        idTokenAndRefreshTokenProvider = new IdTokenAndRefreshTokenProvider();
        validator = new OidcTokenValidator();
    }

    public String getToken() {
        if(tokenIsSoonExpired()) {
            refreshToken();
        }
        return idToken.getIdToken().getToken();
    }

    private void refreshToken() {
        String openAmSessionToken = OpenAmUtils.getSessionToken(srvUsername, srvPassword, konstruerFullstendingAuthUri(openAmHost, authenticateUri));
        String authorizationCode = OpenAmUtils.getAuthorizationCode(openAmHost, openAmSessionToken, openamClientUsername, oidcRedirectUrl);
        IdTokenAndRefreshToken idTokenAndRefreshToken = idTokenAndRefreshTokenProvider.getToken(authorizationCode, oidcRedirectUrl);
        OidcCredential idToken = idTokenAndRefreshToken.getIdToken();
        String jwtToken = idToken.getToken();
        OidcTokenValidatorResult validationResult = validator.validate(jwtToken);

        if (validationResult.isValid()) {
            this.idToken = new IdToken(idToken, validationResult.getExpSeconds());
        } else {
            throw new OidcTokenException("Kunne ikke validere token: "+validationResult.getErrorMessage());
        }
    }

    static String konstruerFullstendingAuthUri(String openAmHost, String authUri ) {
        return openAmHost.replace("oauth2",authUri);
    }

    private boolean tokenIsSoonExpired() {
        return idToken == null || idToken.getExpirationTimeSeconds() * 1000 - Instant.now().toEpochMilli() < getMinimumTimeToExpireBeforeRefresh();
    }

    private int getMinimumTimeToExpireBeforeRefresh() {
        return Integer.parseInt(System.getProperty(REFRESH_TIME, "60")) * 1000;
    }
}
