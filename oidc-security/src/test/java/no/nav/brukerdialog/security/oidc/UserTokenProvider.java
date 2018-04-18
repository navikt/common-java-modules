package no.nav.brukerdialog.security.oidc;


import no.nav.brukerdialog.security.domain.IdTokenAndRefreshToken;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.oidc.provider.IssoOidcProvider;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestUser;

import static no.nav.brukerdialog.security.Constants.*;
import static no.nav.brukerdialog.security.oidc.SystemUserTokenProvider.konstruerFullstendingAuthUri;
import static no.nav.sbl.rest.RestUtils.withClient;

public class UserTokenProvider {

    private static final String USERNAME = System.getProperty("integrasjonstest.brukernavn", "priveligert_veileder");

    public static final String openAmHost = getIssoHostUrl();
    private static final String openamClientUsername = getIssoRpUserUsername();
    private static final String oidcRedirectUrl = getOidcRedirectUrl();
    private static final String authenticateUri = "json/authenticate?authIndexType=service&authIndexValue=adminconsoleservice";

    private final IdTokenAndRefreshTokenProvider idTokenAndRefreshTokenProvider = new IdTokenAndRefreshTokenProvider();
    private final OidcTokenValidator validator = new OidcTokenValidator();
    private final IssoOidcProvider oidcProvider = new IssoOidcProvider();

    public OidcCredential getIdToken() {
        TestUser testUser = FasitUtils.getTestUser(USERNAME);
        return getIdToken(testUser.getUsername(), testUser.getPassword());

    }

    public OidcCredential getIdToken(String username, String password) {
        String openAmSessionToken = withClient(client ->
                OpenAmUtils.getSessionToken(username, password, konstruerFullstendingAuthUri(openAmHost, authenticateUri), client));
        String authorizationCode = withClient(client -> OpenAmUtils.getAuthorizationCode(openAmHost, openAmSessionToken, openamClientUsername, oidcRedirectUrl, client));
        IdTokenAndRefreshToken idTokenAndRefreshToken = idTokenAndRefreshTokenProvider.getToken(authorizationCode, oidcRedirectUrl);
        OidcCredential idToken = idTokenAndRefreshToken.getIdToken();
        OidcTokenValidatorResult validationResult = validator.validate(idToken.getToken(), oidcProvider);

        if (validationResult.isValid()) {
            return idToken;
        } else {
            throw new OidcTokenException("Kunne ikke validere token: "+validationResult.getErrorMessage());
        }
    }

}
