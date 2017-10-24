package no.nav.brukerdialog.security;


import no.nav.brukerdialog.security.oidc.OidcTokenException;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;

import javax.script.ScriptException;
import java.io.IOException;

import static no.nav.brukerdialog.security.Constants.*;
import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_PASSWORD;
import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_USERNAME;

public class OidcTestRunner {

    public static void main(String[] args) throws InterruptedException, ScriptException, IOException, OidcTokenException {

        System.setProperty(ISSO_HOST_URL_PROPERTY_NAME, "https://isso-t.adeo.no/isso/oauth2");
        System.setProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME,"!!CHANGEME!!");
        System.setProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME,"!!CHANGEME!!");
        System.setProperty(ISSO_JWKS_URL,"https://isso-t.adeo.no/isso/oauth2/connect/jwk_uri");
        System.setProperty(ISSO_EXPECTED_TOKEN_ISSUER,"https://isso-t.adeo.no:443/isso/oauth2");
        System.setProperty(OIDC_REDIRECT_URL,"https://localhost:9592/veilarbportefoljeflatefs/api/login");
        System.setProperty(SYSTEMUSER_USERNAME,"!!CHANGEME!!");
        System.setProperty(SYSTEMUSER_PASSWORD,"!!CHANGEME!!");

        SystemUserTokenProvider systemUserTokenProvider = new SystemUserTokenProvider();
        String idToken = systemUserTokenProvider.getToken();

        System.out.println("idToken: " + idToken);
    }
}
