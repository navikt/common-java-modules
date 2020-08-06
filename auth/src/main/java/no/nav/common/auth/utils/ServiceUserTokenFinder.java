package no.nav.common.auth.utils;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.Optional;

/**
 * Denne token finderen henter tokens ut fra Authorization header og sjekker at subject er en systembruker.
 * Denne finderen er kun tenkt å brukes for å validere systembruker tokens fra OpenAM siden de ikke kan skilles på en annen måte.
 */
@Slf4j
public class ServiceUserTokenFinder implements TokenFinder {

    @Override
    public Optional<String> findToken(HttpServletRequest request) {
        Optional<String> maybeToken = TokenUtils.getTokenFromHeader(request);

        if (maybeToken.isPresent()) {
            try {
                JWT jwt = JWTParser.parse(maybeToken.get());
                String subject = jwt.getJWTClaimsSet().getSubject();

                if (subject.startsWith("srv")) {
                    return maybeToken;
                } else {
                    log.warn("ServiceUserTokenFinder found token with invalid subject " + subject);
                }
            } catch (ParseException e) {
                log.error("Failed to parse token", e);
            }
        }

        return Optional.empty();
    }

}
