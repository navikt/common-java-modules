package no.nav.brukerdialog.security.oidc.provider;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.jaspic.TokenLocator;
import no.nav.brukerdialog.security.jwks.JsonWebKeyCache;
import no.nav.brukerdialog.security.jwks.JwtHeader;
import no.nav.sbl.rest.RestUtils;

import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Optional;

import static java.util.Optional.empty;

public class AzureADB2CProvider implements OidcProvider {

    public static final String AZUREADB2C_OIDC_COOKIE_NAME = "selvbetjening-idtoken";
    private static final TokenLocator TOKEN_LOCATOR = new TokenLocator(AZUREADB2C_OIDC_COOKIE_NAME, null);

    private final String expectedAudience;
    private final JsonWebKeyCache keyCache;
    private final String expectedIssuer;

    public AzureADB2CProvider(AzureADB2CConfig azureADB2CConfig) {
        this.expectedAudience = azureADB2CConfig.expectedAudience;
        IssuerMetaData issuerMetaData = RestUtils.withClient(client -> client.target(azureADB2CConfig.discoveryUrl).request().get(IssuerMetaData.class));
        this.expectedIssuer = issuerMetaData.issuer;
        this.keyCache = new JsonWebKeyCache(issuerMetaData.jwks_uri, false);
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
    public Key getVerificationKey(JwtHeader header) {
        return keyCache.getVerificationKey(header);
    }

    @Override
    public Optional<String> getToken(HttpServletRequest httpServletRequest) {
        return TOKEN_LOCATOR.getToken(httpServletRequest);
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
        return IdentType.EksternBruker;
    }

    @SuppressWarnings("unused")
    private static class IssuerMetaData {
        private String issuer;
        private String jwks_uri;
    }
}
