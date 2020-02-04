package no.nav.common.oidc.jaxrs;

import lombok.Value;
import lombok.experimental.Wither;
import no.nav.brukerdialog.security.domain.IdentType;

@Value
@Wither
public class OidcAuthenticatorConfig {

    public String discoveryUrl;

    public String clientId;

    public String idTokenCookieName;

    public IdentType identType;

}
