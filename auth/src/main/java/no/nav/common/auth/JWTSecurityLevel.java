package no.nav.common.auth;

import java.util.Optional;

import static no.nav.common.auth.SecurityLevel.Ukjent;
import static no.nav.common.auth.SecurityLevel.Level1;
import static no.nav.common.auth.SecurityLevel.Level2;
import static no.nav.common.auth.SecurityLevel.Level3;
import static no.nav.common.auth.SecurityLevel.Level4;

public class JWTSecurityLevel {

    private static final String SECURITY_LEVEL_ATTRIBUTE = "acr";
    private final SecurityLevel securityLevel;

    public JWTSecurityLevel(Optional<SsoToken> ssoToken) {
        securityLevel = getOidcSecurityLevel(ssoToken);
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    private SecurityLevel getOidcSecurityLevel(Optional<SsoToken> ssoToken) {
        return ssoToken
                .filter(token -> token.getType() == SsoToken.Type.OIDC)
                .map(SsoToken::getAttributes)
                .map(a -> a.get(SECURITY_LEVEL_ATTRIBUTE))
                .map(o -> o instanceof String ? (String) o : null)
                .map(JWTSecurityLevel::levelFromAcr)
                .orElse(Ukjent);
    }

    private static SecurityLevel levelFromAcr(String acr) {
        switch (acr) {
            case "Level1":
                return Level1;
            case "Level2":
                return Level2;
            case "Level3":
                return Level3;
            case "Level4":
                return Level4;
            default:
                return Ukjent;
        }
    }
}


