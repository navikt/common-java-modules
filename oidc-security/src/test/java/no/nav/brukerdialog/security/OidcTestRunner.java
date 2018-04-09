package no.nav.brukerdialog.security;


import no.nav.brukerdialog.security.oidc.OidcTokenException;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;

import javax.script.ScriptException;
import java.io.IOException;

import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_PASSWORD;
import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_USERNAME;

public class OidcTestRunner {

    public static void main(String[] args) throws InterruptedException, ScriptException, IOException, OidcTokenException {

        System.setProperty("isso-host.url", "https://isso-t.adeo.no/isso/oauth2");
        System.setProperty("isso-rp-user.username", "!!CHANGEME!!");
        System.setProperty("isso-rp-user.password", "!!CHANGEME!!");
        System.setProperty("isso-jwks.url", "https://isso-t.adeo.no/isso/oauth2/connect/jwk_uri");
        System.setProperty("isso-issuer.url", "https://isso-t.adeo.no:443/isso/oauth2");
        System.setProperty("oidc-redirect.url", "https://localhost:9592/veilarbportefoljeflatefs/api/login");
        System.setProperty(SYSTEMUSER_USERNAME,"!!CHANGEME!!");
        System.setProperty(SYSTEMUSER_PASSWORD,"!!CHANGEME!!");

        SystemUserTokenProvider systemUserTokenProvider = new SystemUserTokenProvider();
        String idToken = systemUserTokenProvider.getToken();

        System.out.println("idToken: " + idToken);
    }
}
