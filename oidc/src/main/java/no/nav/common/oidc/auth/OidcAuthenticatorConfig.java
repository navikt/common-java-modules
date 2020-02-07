package no.nav.common.oidc.auth;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import no.nav.brukerdialog.security.domain.IdentType;

@Wither
@NoArgsConstructor
@AllArgsConstructor
public class OidcAuthenticatorConfig {

    public String discoveryUrl;

    public String clientId;

    public String idTokenCookieName;

    public IdentType identType;

}
