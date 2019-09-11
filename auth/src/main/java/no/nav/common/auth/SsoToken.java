package no.nav.common.auth;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static no.nav.common.auth.SsoToken.Type.*;
import static no.nav.util.sbl.AssertUtils.assertNotNull;
import static no.nav.util.sbl.StringUtils.assertNotNullOrEmpty;

@Getter
@EqualsAndHashCode
public class SsoToken {
    private final Type type;
    private final String token;
    private final Map<String, Object> attributes;

    SsoToken(Type type, String token, Map<String, ?> attributes) {
        assertNotNull(type);
        assertNotNull(attributes);
        assertNotNullOrEmpty(token);

        this.type = type;
        this.token = token;
        this.attributes = unmodifiableMap(attributes);
    }

    /**
     * @deprecated use overloaded method - attributes should be provided
     */
    @Deprecated
    public static SsoToken oidcToken(String token) {
        return oidcToken(token, emptyMap());
    }

    public static SsoToken oidcToken(String token, Map<String, ?> attributes) {
        return new SsoToken(OIDC, token, attributes);
    }

    /**
     * @deprecated use overloaded method - attributes should be provided
     */
    @Deprecated
    public static SsoToken saml(String samlAssertion) {
        return saml(samlAssertion, emptyMap());
    }

    public static SsoToken saml(String samlAssertion, Map<String, ?> attributes) {
        return new SsoToken(SAML, samlAssertion, attributes);
    }

    /**
     * @deprecated use overloaded method - attributes should be provided
     */
    @Deprecated
    public static SsoToken eksternOpenAM(String token) {
        return eksternOpenAM(token, emptyMap());
    }

    public static SsoToken eksternOpenAM(String token, Map<String, ?> attributes) {
        return new SsoToken(EKSTERN_OPENAM, token, attributes);
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
