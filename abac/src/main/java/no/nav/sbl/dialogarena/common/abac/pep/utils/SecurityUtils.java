package no.nav.sbl.dialogarena.common.abac.pep.utils;

import no.nav.common.auth.SubjectHandler;

import java.util.Base64;
import java.util.Optional;

import static no.nav.common.auth.SsoToken.Type.OIDC;
import static no.nav.common.auth.SsoToken.Type.SAML;

public class SecurityUtils {

    public static Optional<String> getSamlToken() {
        return SubjectHandler.getSubject()
                .flatMap(subject -> subject.getSsoToken(SAML))
                .map(SecurityUtils::encodeSamlToken);
    }

    public static Optional<String> getOidcToken() {
        return SubjectHandler.getSubject()
                .flatMap(subject -> subject.getSsoToken(OIDC))
                .map(SecurityUtils::extractOidcTokenBody);
    }

    public static String extractOidcTokenBody(String oidcToken) {
        final String[] tokenParts = oidcToken.split("\\.");
        return tokenParts.length == 1 ? tokenParts[0] : tokenParts[1];
    }

    private static String encodeSamlToken(String samlToken) {
        return Base64.getEncoder().encodeToString(samlToken.getBytes());
    }

}
