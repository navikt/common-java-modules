package no.nav.apiapp.security;

import no.nav.common.auth.AuthorizationModule;
import no.nav.common.auth.SecurityLevel;
import no.nav.common.auth.Subject;

import javax.servlet.http.HttpServletRequest;

import java.util.*;

import static no.nav.brukerdialog.security.domain.IdentType.EksternBruker;

public class ApiAppAuthorizationModule implements AuthorizationModule {
    private AuthorizationModule customAuthorizationModule;
    private SecurityLevel defaultSecurityLevel;
    private Map<String, SecurityLevel> pathToSecurityLevel;

    public ApiAppAuthorizationModule(AuthorizationModule customAuthorizationModule,
                                     SecurityLevel defaultSecurityLevel,
                                     Map<SecurityLevel, List<String>> securityLevelForBasePaths) {
        this.customAuthorizationModule = customAuthorizationModule;
        this.defaultSecurityLevel = defaultSecurityLevel;
        this.pathToSecurityLevel = mapAndValidateSecurityLevels(securityLevelForBasePaths);
    }

    private Map<String, SecurityLevel> mapAndValidateSecurityLevels(
            Map<SecurityLevel, List<String>> securityLevelForPaths) {

        HashMap<String, SecurityLevel> map = new HashMap<>();
        securityLevelForPaths.forEach((key, value) -> value.forEach(path -> {
            if (map.containsKey(path)) {
                throw new IllegalStateException("Ambiguous security level for " + path);
            }

            if (!path.matches("^[^/?&=]+$")) {
                throw new IllegalStateException("Invalid path for security level " + path);
            }
            map.put(path, key);
        }));
        return map;
    }

    @Override
    public boolean authorized(Subject subject, HttpServletRequest httpServletRequest) {
        return securityLevelAuthorization(subject, httpServletRequest) && customAuthorization(subject, httpServletRequest);

    }

    private boolean securityLevelAuthorization(Subject subject, HttpServletRequest httpServletRequest) {
        return !EksternBruker.equals(subject.getIdentType()) ||
                securityLevelForPathsAuthorization(subject, httpServletRequest)
                .orElse(defaultSecurityLevelAuthorization(subject, httpServletRequest));
    }

    private boolean defaultSecurityLevelAuthorization(Subject subject, HttpServletRequest httpServletRequest) {
        return Optional.ofNullable(defaultSecurityLevel).map(defaultSecurityLevel ->
                        SecurityLevelAuthorizationModule
                                .authorized(subject, httpServletRequest, defaultSecurityLevel)).orElse(true);
    }

    private Optional<Boolean> securityLevelForPathsAuthorization(Subject subject, HttpServletRequest httpServletRequest) {
        SecurityLevel securityLevel = SecurityLevelAuthorizationModule.getSecurityLevel(subject);
        Optional<SecurityLevel> requiredSecurityLevel = getRequiredSecurityLevel(httpServletRequest);
        return requiredSecurityLevel.map(required -> securityLevel.getSecurityLevel() >= required.getSecurityLevel());
    }

    private Optional<SecurityLevel> getRequiredSecurityLevel(HttpServletRequest httpServletRequest) {
        return getSecurityLevelKey(httpServletRequest).flatMap(str ->
                Optional.ofNullable(pathToSecurityLevel.get(str))
        );
    }

    private Optional<String> getSecurityLevelKey(HttpServletRequest httpServletRequest) {
        return Optional.ofNullable(httpServletRequest.getPathInfo())
                .map(str -> str.substring(1))
                .map(str -> {
                    if (str.indexOf('/') != -1) return str.substring(0, str.indexOf('/'));
                    else return str;
                });
    }

    private boolean customAuthorization(Subject subject, HttpServletRequest httpServletRequest) {
        return Optional.ofNullable(customAuthorizationModule)
                .map(authModule -> authModule.authorized(subject, httpServletRequest))
                .orElse(true);
    }
}
