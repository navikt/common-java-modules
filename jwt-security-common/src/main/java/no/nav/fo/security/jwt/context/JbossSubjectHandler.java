package no.nav.fo.security.jwt.context;

import org.jboss.security.SecurityContextAssociation;

import javax.security.auth.Subject;

public class JbossSubjectHandler extends SubjectHandler {

    @Override
    public Subject getSubject() {
        return SecurityContextAssociation.getSubject();
    }

}
