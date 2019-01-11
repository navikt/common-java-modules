package no.nav.brukerdialog.security.oidc.provider;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.jwks.CacheMissAction;
import no.nav.brukerdialog.security.jwks.JsonWebKeyCache;
import no.nav.brukerdialog.security.jwks.JwtHeader;
import no.nav.brukerdialog.security.oidc.OidcTokenValidator;
import no.nav.sbl.rest.RestUtils;
import no.nav.sbl.util.EnumUtils;

import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Optional;

import static no.nav.brukerdialog.security.jaspic.TokenLocator.getTokenFromHeader;
import static no.nav.brukerdialog.security.oidc.OidcTokenUtils.getStringFieldFromToken;
import static no.nav.sbl.util.StringUtils.assertNotNullOrEmpty;

public class SecurityTokenServiceOidcProvider implements OidcProvider {

    private final String expectedIssuer;
    private final JsonWebKeyCache keyCache;

    public SecurityTokenServiceOidcProvider(SecurityTokenServiceOidcProviderConfig securityTokenServiceOidcProviderConfig) {
        Configuration configuration = RestUtils.withClient(c -> c.target(securityTokenServiceOidcProviderConfig.discoveryUrl).request().get(Configuration.class));
        this.expectedIssuer = assertNotNullOrEmpty(configuration.issuer);
        this.keyCache = new JsonWebKeyCache(configuration.jwks_uri, true);
    }

    @Override
    public Optional<String> getToken(HttpServletRequest httpServletRequest) {
        return getTokenFromHeader(httpServletRequest);
    }

    @Override
    public Optional<String> getRefreshToken(HttpServletRequest httpServletRequest) {
        return Optional.empty(); // not supported
    }

    @Override
    public OidcCredential getFreshToken(String refreshToken, String requestToken) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public Optional<Key> getVerificationKey(JwtHeader header, CacheMissAction cacheMissAction) {
        return keyCache.getVerificationKey(header, cacheMissAction);
    }

    @Override
    public String getExpectedIssuer() {
        return expectedIssuer;
    }

    @Override
    public String getExpectedAudience(String token) {
        return null;  // We intentionally expect any or no audience when validating internal oidc tokens
    }

    @Override
    public IdentType getIdentType(String token) {
        String identType = getStringFieldFromToken(token, "identType");
        return EnumUtils.valueOf(IdentType.class, identType).orElseThrow(() -> new IllegalStateException("invalid identType: " + identType));
    }

    @SuppressWarnings("unused")
    private static class Configuration {
        private String issuer;
        private String jwks_uri;
    }

}
