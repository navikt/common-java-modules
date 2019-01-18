package no.nav.common.auth;

import javax.servlet.http.HttpServletRequest;

public interface AuthorizationModule {
    boolean authorized(Subject subject, HttpServletRequest httpServletRequest);
}
