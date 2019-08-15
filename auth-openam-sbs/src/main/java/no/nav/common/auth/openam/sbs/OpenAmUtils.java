package no.nav.common.auth.openam.sbs;

import no.nav.common.auth.SecurityLevel;
import no.nav.common.auth.SsoToken;

import static java.util.Optional.ofNullable;
import static no.nav.common.auth.SecurityLevel.*;

public class OpenAmUtils {

    public static SecurityLevel getSecurityLevel(SsoToken ssoToken) {
        return ssoToken.getType() != SsoToken.Type.EKSTERN_OPENAM ? Ukjent : ofNullable(ssoToken.getAttributes())
                .map(a -> a.get(OpenAMUserInfoService.PARAMETER_SECURITY_LEVEL))
                .map(p -> p instanceof String ? (String) p : null)
                .map(OpenAmUtils::parameterToSecurityLevel)
                .orElse(Ukjent);
    }

    private static SecurityLevel parameterToSecurityLevel(String securityLevel) {
        if (securityLevel == null) {
            return Ukjent;
        }

        switch (securityLevel) {
            case "1":
                return Level1;
            case "2":
                return Level2;
            case "3":
                return Level3;
            case "4":
                return Level4;
            default:
                return Ukjent;
        }
    }

}
