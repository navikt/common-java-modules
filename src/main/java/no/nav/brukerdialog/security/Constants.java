package no.nav.brukerdialog.security;

import static java.lang.System.getProperty;

public class Constants {

    public static final String ID_TOKEN_COOKIE_NAME = "ID_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    public static final String REFRESH_TIME = "no.nav.brukerdialog.security.oidc.minimum_time_to_expire_before_refresh.seconds";

    public static String getIssoHostUrl() {
        return getProperty("isso-host.url", getProperty("ISSO_HOST_URL"));
    }
    public static String getIssoRpUserUsername() {
        return getProperty("isso-rp-user.username", getProperty("ISSO_RP_USER_USERNAME"));
    }
    public static String getIssoRpUserPassword() {
        return getProperty("isso-rp-user.password", getProperty("ISSO_RP_USER_PASSWORD"));
    }

    public static String getIssoExpectedTokenIssuer() {
        return getProperty("isso-issuer.url", getProperty("ISSO_ISSUER_URL"));
    }
    public static String getOidcRedirectUrl() {
        return getProperty("oidc-redirect.url", getProperty("OIDC_REDIRECT_URL"));
    }
    public static String getIssoJwksUrl() {
        return getProperty("isso-jwks.url", getProperty("ISSO_JWKS_URL"));
    }
    public static String getIssoIsaliveUrl() {
        return getProperty("isso.isalive.url", getProperty("ISSO_ISALIVE_URL"));
    }
}
