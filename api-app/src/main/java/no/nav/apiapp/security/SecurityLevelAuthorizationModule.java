package no.nav.apiapp.security;

import no.nav.brukerdialog.security.oidc.OidcTokenUtils;
import no.nav.common.auth.AuthorizationModule;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.common.auth.openam.sbs.OpenAmUtils;

import javax.servlet.http.HttpServletRequest;

public class SecurityLevelAuthorizationModule implements AuthorizationModule {

    private final int minimumLevel;

    public SecurityLevelAuthorizationModule(int minimumLevel) {
        this.minimumLevel = minimumLevel;
    }

    @Override
    public boolean authorized(Subject subject, HttpServletRequest httpServletRequest) {
        return subject != null && getSecurityLevel(subject) >= minimumLevel;
    }

    public static boolean authorized(Subject subject, HttpServletRequest httpServletRequest, int minimumLevel) {
        return subject != null && getSecurityLevel(subject) >= minimumLevel;
    }

    private static int getSecurityLevel(Subject subject) {
        SsoToken ssoToken = subject.getSsoToken();
        switch (ssoToken.getType()) {
            case OIDC:
                return OidcTokenUtils.getOidcSecurityLevel(ssoToken).getSecurityLevel();
            case EKSTERN_OPENAM:
                return OpenAmUtils.getSecurityLevel(ssoToken).getSecurityLevel();
            default:
                return Integer.MIN_VALUE;
        }
    }

}
