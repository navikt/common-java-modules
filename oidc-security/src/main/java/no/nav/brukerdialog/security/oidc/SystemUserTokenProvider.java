package no.nav.brukerdialog.security.oidc;


import no.nav.brukerdialog.security.domain.IdToken;
import no.nav.brukerdialog.security.domain.IdTokenAndRefreshToken;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.oidc.provider.IssoOidcProvider;
import no.nav.brukerdialog.security.oidc.provider.IssoOidcProviderConfig;
import no.nav.sbl.rest.RestUtils;

import javax.ws.rs.client.Client;
import java.time.Instant;

import static no.nav.brukerdialog.security.Constants.REFRESH_TIME;

public class SystemUserTokenProvider {

    private static final String authenticateUri = "json/authenticate?authIndexType=service&authIndexValue=adminconsoleservice";

    private final SystemUserTokenProviderConfig config;

    private final String srvUsername;
    private final String srvPassword;
    private final String openAmHost;
    private final String openamClientUsername;
    private final String oidcRedirectUrl;

    private final Client client = RestUtils.createClient();

    private final IdTokenAndRefreshTokenProvider idTokenAndRefreshTokenProvider;
    private final OidcTokenValidator validator;
    private final IssoOidcProvider oidcProvider;

    private IdToken idToken;

    public SystemUserTokenProvider() {
        this(SystemUserTokenProviderConfig.resolveFromSystemProperties());
    }

    public SystemUserTokenProvider(SystemUserTokenProviderConfig systemUserTokenProviderConfig) {
        this.config = systemUserTokenProviderConfig;

        this.srvUsername = systemUserTokenProviderConfig.srvUsername;
        this.srvPassword = systemUserTokenProviderConfig.srvPassword;
        this.openAmHost = systemUserTokenProviderConfig.issoHostUrl;
        this.openamClientUsername = systemUserTokenProviderConfig.issoRpUserUsername;
        this.oidcRedirectUrl = systemUserTokenProviderConfig.oidcRedirectUrl;

        this.idTokenAndRefreshTokenProvider = new IdTokenAndRefreshTokenProvider(IdTokenAndRefreshTokenProviderConfig.from(systemUserTokenProviderConfig));
        this.validator = new OidcTokenValidator();
        this.oidcProvider = new IssoOidcProvider(IssoOidcProviderConfig.from(systemUserTokenProviderConfig));
    }

    public SystemUserTokenProviderConfig getConfig() {
        return config;
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
