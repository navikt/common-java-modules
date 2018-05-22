package no.nav.brukerdialog.security.oidc.provider;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.jwks.JwtHeader;
import no.nav.sbl.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Optional;

public interface OidcProvider {

    default boolean match(HttpServletRequest httpServletRequest){
        return getToken(httpServletRequest).filter(StringUtils::notNullOrEmpty).isPresent();
    }

    Optional<String> getToken(HttpServletRequest httpServletRequest);
    Optional<String> getRefreshToken(HttpServletRequest httpServletRequest);
    OidcCredential getFreshToken(String refreshToken, String requestToken);
    Key getVerificationKey(JwtHeader header);
    String getExpectedIssuer();
    String getExpectedAudience(String token);
    IdentType getIdentType(String token);

}
