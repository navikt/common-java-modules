package no.nav.modig.security.ws.attributes;

import no.nav.modig.core.domain.ConsumerId;
import no.nav.modig.core.domain.IdentType;

/**
 * Fetches SAML attributes for user
 *
 * NOT A PART OF THE PUBLIC API
 */
public class SystemSAMLAttributes implements SAMLAttributes {

    @Override
    public String getUid() {
        return new ConsumerId().getConsumerId();
    }

    @Override
    public String getAuthenticationLevel() {
        return "0";
    }

    @Override
    public String getIdentType() {
        return IdentType.Systemressurs.name();
    }

    @Override
    public String getConsumerId() {
        return new ConsumerId().getConsumerId();
    }
}
