package no.nav.apiapp.security;

import no.nav.brukerdialog.security.SecurityLevel;
import no.nav.common.auth.AuthorizationModule;
import no.nav.common.auth.Subject;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

import static no.nav.brukerdialog.security.domain.IdentType.EksternBruker;

public class ApiAppAuthorizationModule implements AuthorizationModule {
    private AuthorizationModule customAuthorizationModule;
    private SecurityLevel defaultSecurityLevel;

    public ApiAppAuthorizationModule(AuthorizationModule customAuthorizationModule, SecurityLevel defaultSecurityLevel) {
        this.customAuthorizationModule = customAuthorizationModule;
        this.defaultSecurityLevel = defaultSecurityLevel;
    }

    @Override
    public boolean authorized(Subject subject, HttpServletRequest httpServletRequest) {
        return customAuth(subject, httpServletRequest) && securityLevelAuth(subject, httpServletRequest);

    }

    private boolean customAuth(Subject subject, HttpServletRequest httpServletRequest) {
        return Optional.ofNullable(customAuthorizationModule)
                .map(authModule -> authModule.authorized(subject, httpServletRequest))
                .orElse(true);
    }

    private boolean securityLevelAuth(Subject subject, HttpServletRequest httpServletRequest) {
        return !subject.getIdentType().equals(EksternBruker) ||
                SecurityLevelAuthorizationModule
                        .authorized(subject, httpServletRequest, defaultSecurityLevel.getSecurityLevel());
    }
}
