package no.nav.brukerdialog.security.context;

import no.nav.brukerdialog.security.domain.ConsumerId;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.domain.SluttBruker;

import javax.security.auth.Subject;

public class InternbrukerSubjectHandler extends TestSubjectHandler {
    private static final Subject DEFAULT_SUBJECT;
    private static String veilederIdent;
    private static String serviceBruker;

    public InternbrukerSubjectHandler(String vIdent, String srvBruker) {
        veilederIdent = vIdent;
        serviceBruker = srvBruker;
    }

    static {
        DEFAULT_SUBJECT = new Subject();
        DEFAULT_SUBJECT.getPrincipals().add(new SluttBruker(veilederIdent, IdentType.InternBruker));
        DEFAULT_SUBJECT.getPrincipals().add(new ConsumerId(serviceBruker));
    }

    private static Subject subject = DEFAULT_SUBJECT;

    @Override
    public Subject getSubject() {
        return subject;
    }

    @Override
    public void setSubject(Subject newSubject) {
        subject = newSubject;
    }

    @Override
    public void reset() {
        setSubject(DEFAULT_SUBJECT);
    }
}