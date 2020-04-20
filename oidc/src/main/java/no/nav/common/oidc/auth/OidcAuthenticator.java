package no.nav.common.oidc.auth;

import lombok.Value;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.oidc.OidcTokenValidator;
import no.nav.common.oidc.utils.TokenFinder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Value
public class OidcAuthenticator {

    public OidcTokenValidator tokenValidator;

    public OidcAuthenticatorConfig config;

    public static OidcAuthenticator fromConfig(OidcAuthenticatorConfig config) {
        if (!config.isValid()) {
            throw new IllegalStateException("OidcAuthenticatorConfig is missing one or more values");
        }

        OidcTokenValidator validator = new OidcTokenValidator(config.discoveryUrl, config.clientId);
        return new OidcAuthenticator(validator, config);
    }

    public Optional<String> findIdToken(HttpServletRequest request) {
        return config.idTokenFinder.findToken(request);
    }

    public Optional<String> findRefreshToken(HttpServletRequest request) {
        if (config.refreshTokenFinder != null) {
            return config.refreshTokenFinder.findToken(request);
        }

        return Optional.empty();
    }

}
