package no.nav.common.auth.utils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

@Deprecated
public class JavaxAuthHeaderTokenFinder implements JavaxTokenFinder {

    @Override
    public Optional<String> findToken(HttpServletRequest request) {
        return JavaxTokenUtils.getTokenFromHeader(request);
    }

}
