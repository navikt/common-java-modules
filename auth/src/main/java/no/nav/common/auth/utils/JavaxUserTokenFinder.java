package no.nav.common.auth.utils;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Denne token finderen henter tokens ut fra Authorization header og sjekker at subject ikke er en systembruker.
 * Denne finderen er kun tenkt å brukes for å validere tokens fra OpenAM siden de ikke kan skilles på en annen måte.
 */
@Deprecated
public class JavaxUserTokenFinder implements JavaxTokenFinder {

    @Override
    public Optional<String> findToken(HttpServletRequest request) {
        Optional<String> maybeToken = JavaxTokenUtils.getTokenFromHeader(request);

        if (maybeToken.isPresent() && !JavaxTokenUtils.isServiceUserToken(maybeToken.get())) {
            return maybeToken;
        }

        return Optional.empty();
    }

}
