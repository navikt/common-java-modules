package no.nav.security.jwt.security.context;

import javax.security.auth.Subject;

/**
 * <p>
 * A SubjectHandler that holds the Subject in a ThreadLocal field.
 * </p>
 * <p>
 * <p>
 * Use this SubjectHandler in Jetty and tests where the Subject matters.
 * </p>
 *
 * @see StaticSubjectHandler
 */
public class ThreadLocalSubjectHandler extends TestSubjectHandler {

    private static ThreadLocal<Subject> subjectHolder = new ThreadLocal<>();

    @Override
    public Subject getSubject() {
        return subjectHolder.get();
    }

    @Override
    public void setSubject(Subject subject) {
        subjectHolder.set(subject);
    }

    /**
     * Sets the Subject to <code>null</code>
     */
    @Override
    public void reset() {
        setSubject(null);
    }
}
