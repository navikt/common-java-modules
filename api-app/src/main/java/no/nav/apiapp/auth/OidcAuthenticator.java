package no.nav.apiapp.auth;

import lombok.Value;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.oidc.OidcTokenValidator;
import no.nav.common.oidc.TokenLocator;

@Value
public class OidcAuthenticator {

    public OidcTokenValidator tokenValidator;

    public TokenLocator idTokenLocator;

    public IdentType identType;

}
