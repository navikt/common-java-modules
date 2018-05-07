package no.nav.brukerdialog.security;

import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class Constants {

    public static final String ID_TOKEN_COOKIE_NAME = "ID_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    public static final String REFRESH_TIME = "no.nav.brukerdialog.security.oidc.minimum_time_to_expire_before_refresh.seconds";

    public static final String ISSO_HOST_URL_PROPERTY_NAME = "isso-host.url";
    public static final String ISSO_RP_USER_USERNAME_PROPERTY_NAME = "isso-rp-user.username";
    public static final String ISSO_RP_USER_PASSWORD_PROPERTY_NAME = "isso-rp-user.password";
    public static final String ISSO_ISSUER_URL_PROPERTY_NAME = "isso-issuer.url";
    public static final String OIDC_REDIRECT_URL_PROPERTY_NAME = "oidc-redirect.url";
    public static final String ISSO_JWKS_URL_PROPERTY_NAME = "isso-jwks.url";
    public static final String ISSO_ISALIVE_URL_PROPERTY_NAME = "isso.isalive.url";

    public static String getIssoHostUrl() {
        return getRequiredProperty(ISSO_HOST_URL_PROPERTY_NAME, "ISSO_HOST_URL");
    }
    public static String getIssoRpUserUsername() {
        return getRequiredProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME, "ISSO_RP_USER_USERNAME");
    }
    public static String getIssoRpUserPassword() {
        return getRequiredProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME, "ISSO_RP_USER_PASSWORD");
    }

    public static String getIssoExpectedTokenIssuer() {
        return getRequiredProperty(ISSO_ISSUER_URL_PROPERTY_NAME, "ISSO_ISSUER_URL");
    }
    public static String getOidcRedirectUrl() {
        return getRequiredProperty(OIDC_REDIRECT_URL_PROPERTY_NAME, "OIDC_REDIRECT_URL");
    }
    public static boolean hasRedirectUrl() {
        return getOptionalProperty(OIDC_REDIRECT_URL_PROPERTY_NAME, "OIDC_REDIRECT_URL").isPresent();
    }
    public static String getIssoJwksUrl() {
        return getRequiredProperty(ISSO_JWKS_URL_PROPERTY_NAME, "ISSO_JWKS_URL");
    }
    public static String getIssoIsaliveUrl() {
        return getRequiredProperty(ISSO_ISALIVE_URL_PROPERTY_NAME, "ISSO_ISALIVE_URL");
    }
}
