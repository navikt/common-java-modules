package no.nav.common.auth.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Denne token finderen henter tokens ut fra Authorization header og sjekker at subject er en systembruker.
 * Denne finderen er kun tenkt å brukes for å validere systembruker tokens fra OpenAM siden de ikke kan skilles på en annen måte.
 */
public class ServiceUserTokenFinder implements TokenFinder {

    @Override
    public Optional<String> findToken(HttpServletRequest request) {
        Optional<String> maybeToken = TokenUtils.getTokenFromHeader(request);

        if (maybeToken.isPresent() && TokenUtils.isServiceUserToken(maybeToken.get())) {
            return maybeToken;
        }

        return Optional.empty();
    }
}
