package no.nav.brukerdialog.security.oidc.provider;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.jaspic.TokenLocator;
import no.nav.brukerdialog.security.jwks.JsonWebKeyCache;
import no.nav.brukerdialog.security.jwks.JwtHeader;
import no.nav.brukerdialog.security.oidc.IdTokenProvider;
import no.nav.brukerdialog.security.oidc.OidcTokenUtils;

import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Optional;

import static no.nav.brukerdialog.security.Constants.*;
import static no.nav.brukerdialog.security.oidc.OidcTokenUtils.getOpenamClientFromToken;

public class IssoOidcProvider implements OidcProvider {

    private final TokenLocator tokenLocator = new TokenLocator(ID_TOKEN_COOKIE_NAME, REFRESH_TOKEN_COOKIE_NAME);
    private final IdTokenProvider idTokenProvider = new IdTokenProvider();
    private final JsonWebKeyCache keyCache = new JsonWebKeyCache(getIssoJwksUrl(), true);

    @Override
    public String getExpectedIssuer() {
        return getIssoExpectedTokenIssuer();
    }

    @Override
    public String getExpectedAudience(String token) {
        //Biblioteket støtter ikke at man disabler expected audience sjekken dersom den finnes en "aud"-claim i tokenet.
        //Henter dermed ut aud fra tokenet og setter det som expected.
        return OidcTokenUtils.getTokenAud(token);
    }

    @Override
    public Key getVerificationKey(JwtHeader header) {
        return keyCache.getVerificationKey(header);
    }

    @Override
    public Optional<String> getToken(HttpServletRequest httpServletRequest) {
        return tokenLocator.getToken(httpServletRequest);
    }

    @Override
    public Optional<String> getRefreshToken(HttpServletRequest httpServletRequest) {
        return tokenLocator.getRefreshToken(httpServletRequest);
    }

    @Override
    public OidcCredential getFreshToken(String refreshToken, String requestToken) {
        return idTokenProvider.getToken(refreshToken, getOpenamClientFromToken(requestToken));
    }

    @Override
    public IdentType getIdentType(String token) {
        return IdentType.InternBruker;
    }

}
