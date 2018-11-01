package no.nav.common.auth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import no.nav.sbl.util.AssertUtils;

import static no.nav.common.auth.SsoToken.Type.*;
import static no.nav.sbl.util.AssertUtils.assertNotNull;
import static no.nav.sbl.util.StringUtils.assertNotNullOrEmpty;

@Getter
@EqualsAndHashCode
public class SsoToken {
    private final Type type;
    private final String token;

    public SsoToken(Type type, String token) {
        assertNotNull(type);
        assertNotNullOrEmpty(token);

        this.type = type;
        this.token = token;
    }

    public static SsoToken oidcToken(String token) {
        return new SsoToken(OIDC, token);
    }

    public static SsoToken saml(String samlAssertion) {
        return new SsoToken(SAML, samlAssertion);
    }

    public static SsoToken eksternOpenAM(String token) {
        return new SsoToken(EKSTERN_OPENAM, token);
    }

    public enum Type {
        OIDC,
        EKSTERN_OPENAM,
        SAML
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
