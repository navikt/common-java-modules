package no.nav.apiapp.security;

import no.nav.brukerdialog.security.oidc.OidcTokenUtils;
import no.nav.common.auth.AuthorizationModule;
import no.nav.common.auth.SecurityLevel;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.common.auth.openam.sbs.OpenAmUtils;

import javax.servlet.http.HttpServletRequest;

public class SecurityLevelAuthorizationModule implements AuthorizationModule {

    private final SecurityLevel minimumLevel;

    public SecurityLevelAuthorizationModule(SecurityLevel minimumLevel) {
        this.minimumLevel = minimumLevel;
    }

    @Override
    public boolean authorized(Subject subject, HttpServletRequest httpServletRequest) {
        return authorized(subject, httpServletRequest, minimumLevel);
    }

    public static boolean authorized(Subject subject, HttpServletRequest httpServletRequest, SecurityLevel minimumLevel) {
        return subject != null && getSecurityLevel(subject).getSecurityLevel() >= minimumLevel.getSecurityLevel();
    }

    public static SecurityLevel getSecurityLevel(Subject subject) {
        SsoToken ssoToken = subject.getSsoToken();
        switch (ssoToken.getType()) {
            case OIDC:
                return OidcTokenUtils.getOidcSecurityLevel(ssoToken);
            case EKSTERN_OPENAM:
                return OpenAmUtils.getSecurityLevel(ssoToken);
            default:
                return SecurityLevel.Ukjent;
        }
    }

}
