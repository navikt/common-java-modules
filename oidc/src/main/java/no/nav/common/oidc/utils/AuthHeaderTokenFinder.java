package no.nav.common.oidc.utils;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public class AuthHeaderTokenFinder implements TokenFinder {

    @Override
    public Optional<String> findToken(HttpServletRequest request) {
        return TokenUtils.getTokenFromHeader(request);
    }

}
