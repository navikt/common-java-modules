package no.nav.common.auth.utils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public class AuthHeaderTokenFinder implements TokenFinder {

    @Override
    public Optional<String> findToken(HttpServletRequest request) {
        return TokenUtils.getTokenFromHeader(request);
    }

}
