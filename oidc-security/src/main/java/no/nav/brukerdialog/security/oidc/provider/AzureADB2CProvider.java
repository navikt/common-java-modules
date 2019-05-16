package no.nav.brukerdialog.security.oidc.provider;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.jaspic.TokenLocator;
import no.nav.brukerdialog.security.jwks.CacheMissAction;
import no.nav.brukerdialog.security.jwks.JsonWebKeyCache;
import no.nav.brukerdialog.security.jwks.JwtHeader;
import no.nav.sbl.rest.RestUtils;

import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Optional;

import static java.util.Optional.empty;

public class AzureADB2CProvider implements OidcProvider {

    private final TokenLocator tokenLocator;
    private final IdentType identType;

    private final String expectedAudience;
    private final JsonWebKeyCache keyCache;
    private final String expectedIssuer;

    public AzureADB2CProvider(AzureADB2CConfig config) {
        this.expectedAudience = config.expectedAudience;
        IssuerMetaData issuerMetaData = RestUtils.withClient(client -> client.target(config.discoveryUrl).request().get(IssuerMetaData.class));
        this.expectedIssuer = issuerMetaData.issuer;
        this.keyCache = new JsonWebKeyCache(issuerMetaData.jwks_uri, false);
        this.tokenLocator = new TokenLocator(config.tokenName, null);
        this.identType = config.identType;
    }

    @Override
    public String getExpectedIssuer() {
        return expectedIssuer;
    }

    @Override
    public String getExpectedAudience(String token) {
        return expectedAudience;
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
        return empty(); // not supported
    }

    @Override
    public OidcCredential getFreshToken(String refreshToken, String openamClient) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public IdentType getIdentType(String token) {
        return this.identType;
    }

    @SuppressWarnings("unused")
    private static class IssuerMetaData {
        private String issuer;
        private String jwks_uri;
    }
}
