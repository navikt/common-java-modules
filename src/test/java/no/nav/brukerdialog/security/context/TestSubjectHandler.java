package no.nav.brukerdialog.security.context;

import javax.security.auth.Subject;

public abstract class TestSubjectHandler extends SubjectHandler {

    public abstract void setSubject(Subject subject);

    /**
     * Resets the subject to default value which may be null or a concrete subject
     *
     * @see ThreadLocalSubjectHandler#reset()
     * @see StaticSubjectHandler#reset()
     */
    public abstract void reset();
}
