package no.nav.common.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

public interface LoginProvider {
    Optional<Subject> authenticate(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse);
    Optional<String> redirectUrl(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse);
}
