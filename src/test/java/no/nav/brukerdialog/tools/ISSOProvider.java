package no.nav.brukerdialog.tools;

import no.nav.brukerdialog.security.oidc.UserTokenProvider;

import static java.lang.System.getProperty;

public class ISSOProvider {

    public static String getIDToken(String username, String password) {
        return new UserTokenProvider().getIdToken(username, password).getToken();
    }

    @Deprecated
    //send inn username og passord direkte så slik at oidc-security ikke trenger å ha et forhold til dem
    public static String getIDToken() {
        return getIDToken(getProperty("veileder.username"), getProperty("veileder.password"));
    }
}
