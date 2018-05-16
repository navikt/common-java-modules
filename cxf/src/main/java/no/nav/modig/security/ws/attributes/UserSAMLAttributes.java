package no.nav.modig.security.ws.attributes;

import no.nav.modig.core.context.SubjectHandler;
import no.nav.modig.core.domain.ConsumerId;

/**
 * Fetches SAML attributes for user
 *
 * NOT A PART OF THE PUBLIC API
 */
public class UserSAMLAttributes implements SAMLAttributes {

    @Override
    public String getUid() {
        SubjectHandler subjectHandler = SubjectHandler.getSubjectHandler();
        return subjectHandler.getUid();
    }

    @Override
    public String getAuthenticationLevel() {
        SubjectHandler subjectHandler = SubjectHandler.getSubjectHandler();
        return subjectHandler.getAuthenticationLevel().toString();
    }

    @Override
    public String getIdentType() {
        SubjectHandler subjectHandler = SubjectHandler.getSubjectHandler();
        return subjectHandler.getIdentType().name();
    }

    @Override
    public String getConsumerId() {
        ConsumerId consumerId = new ConsumerId();
        return consumerId.getConsumerId();
    }
    
}
