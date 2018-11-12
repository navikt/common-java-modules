package no.nav.brukerdialog.security.oidc.provider;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.jaspic.TokenLocator;
import no.nav.brukerdialog.security.jwks.CacheMissAction;
import no.nav.brukerdialog.security.jwks.JsonWebKeyCache;
import no.nav.brukerdialog.security.jwks.JwtHeader;
import no.nav.brukerdialog.security.oidc.IdTokenProvider;
import no.nav.brukerdialog.security.oidc.IdTokenProviderConfig;
import no.nav.brukerdialog.security.oidc.OidcTokenUtils;

import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Optional;

import static no.nav.brukerdialog.security.Constants.*;
import static no.nav.brukerdialog.security.oidc.OidcTokenUtils.getOpenamClientFromToken;

public class IssoOidcProvider implements OidcProvider {

    private final TokenLocator tokenLocator = new TokenLocator(ID_TOKEN_COOKIE_NAME, REFRESH_TOKEN_COOKIE_NAME);
    private final IdTokenProvider idTokenProvider;
    private final JsonWebKeyCache keyCache;
    private final String issoExpectedTokenIssuer;

    public IssoOidcProvider() {
        this(IssoOidcProviderConfig.resolveFromSystemProperties());
    }

    public IssoOidcProvider(IssoOidcProviderConfig issoOidcProviderConfig) {
        this.issoExpectedTokenIssuer = issoOidcProviderConfig.issoExpectedTokenIssuer;
        this.idTokenProvider = new IdTokenProvider(IdTokenProviderConfig.from(issoOidcProviderConfig));
        this.keyCache = new JsonWebKeyCache(issoOidcProviderConfig.issoJwksUrl, true);
    }

    @Override
    public String getExpectedIssuer() {
        return issoExpectedTokenIssuer;
    }

    @Override
    public String getExpectedAudience(String token) {
        return null; // We intentionally expect any or no audience when validating internal oidc tokens
    }

    @Override
    public Optional<Key> getVerificationKey(JwtHeader header, CacheMissAction cacheMissAction) {
        return keyCache.getVerificationKey(header, cacheMissAction);
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
