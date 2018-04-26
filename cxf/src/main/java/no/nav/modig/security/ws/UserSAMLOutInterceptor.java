package no.nav.modig.security.ws;

import static org.apache.wss4j.common.ConfigurationConstants.SAML_CALLBACK_REF;

import no.nav.modig.security.ws.attributes.UserSAMLAttributes;

import java.util.Map;

/**
 * CXF Soap interceptor som propagerer sikkerhetskontekst i form av et SAML token.
 * Propagerer sluttbrukers sikkerhetskontekst.
 *
 * Konfigureres med securityOutInterceptor.properties som holder følgende properties:
 *
 * org.apache.wss4j.crypto.merlin.keystore.file - default hentes fra system properties
 * org.apache.wss4j.crypto.merlin.keystore.password - default hentes fra system properties
 * org.apache.ws.security.saml.issuer.key.name (privatekeyentry) 
 * org.apache.ws.security.saml.issuer.key.password - default hentes fra system properties
 * org.apache.ws.security.saml.issuer (applicationcert)
 * org.apache.ws.security.saml.issuer.sendKeyValue (false)
 *
 */
public class UserSAMLOutInterceptor extends AbstractSAMLOutInterceptor {
    public UserSAMLOutInterceptor() {
        super(true);
        getProperties().put(SAML_CALLBACK_REF, getCallbackHandler());
    }

    public UserSAMLOutInterceptor(Map<String, Object> props) {
        super(true, props);
        getProperties().put(SAML_CALLBACK_REF, getCallbackHandler());
    }

    protected SAMLCallbackHandler getCallbackHandler() {
        return new SAMLCallbackHandler(new UserSAMLAttributes());
    }
}
