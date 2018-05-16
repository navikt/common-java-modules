package no.nav.modig.core.context;

import javax.security.auth.Subject;

import no.nav.modig.core.domain.ConsumerId;
import no.nav.modig.core.domain.SluttBruker;

/**
 * <p>
 * A SubjectHandler that holds the Subject in a static field. It also has a default Subject.
 * </p>
 * 
 * <p>
 * Use this SubjectHandler if you just need a Subjecthandler and don't care about the Subject or if you need it shared across
 * threads.
 * </p>
 * 
 * @see ThreadLocalSubjectHandler
 * 
 */
public class StaticSubjectHandler extends SubjectHandler  {
    private static final Subject DEFAULT_SUBJECT;
    static {
        DEFAULT_SUBJECT = new Subject();
        DEFAULT_SUBJECT.getPrincipals().add(SluttBruker.eksternBruker("01015245464"));
        DEFAULT_SUBJECT.getPrincipals().add(new ConsumerId("StaticSubjectHandlerConsumerId"));
        DEFAULT_SUBJECT.getPublicCredentials().add(new OpenAmTokenCredential("01015245464-4"));
        DEFAULT_SUBJECT.getPublicCredentials().add(new AuthenticationLevelCredential(4));
    }
    private static Subject subject = DEFAULT_SUBJECT;

    @Override
    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject newSubject) {
        subject = newSubject;
    }

    public void reset() {
        setSubject(DEFAULT_SUBJECT);
    }

}