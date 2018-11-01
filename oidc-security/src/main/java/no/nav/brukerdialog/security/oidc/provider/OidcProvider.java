package no.nav.brukerdialog.security.oidc.provider;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.jwks.CacheMissAction;
import no.nav.brukerdialog.security.jwks.JwtHeader;

import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Optional;

public interface OidcProvider {

    Optional<String> getToken(HttpServletRequest httpServletRequest);
    Optional<String> getRefreshToken(HttpServletRequest httpServletRequest);
    OidcCredential getFreshToken(String refreshToken, String requestToken);
    Optional<Key> getVerificationKey(JwtHeader header, CacheMissAction cacheMissAction);
    String getExpectedIssuer();
    String getExpectedAudience(String token);
    IdentType getIdentType(String token);

}
