package no.nav.common.auth;

import static java.util.Optional.*;

public enum SecurityLevel {
    Level1(1),
    Level2(2),
    Level3(3),
    Level4(4),
    Ukjent(-1);

    private static final String SECURITY_LEVEL_ATTRIBUTE = "acr";

    private int securityLevel;

    SecurityLevel(int securityLevel) {
        this.securityLevel = securityLevel;
    }

    public int getSecurityLevel() {
        return securityLevel;
    }

    public static SecurityLevel getOidcSecurityLevel(SsoToken ssoToken) {
        return of(ssoToken)
                .filter(token -> token.getType() == SsoToken.Type.OIDC)
                .map(SsoToken::getAttributes)
                .map(a -> a.get(SECURITY_LEVEL_ATTRIBUTE))
                .map(o -> o instanceof String ? (String) o : null)
                .map(SecurityLevel::levelFromAcr)
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
