package no.nav.brukerdialog.security.context;

import no.nav.brukerdialog.security.domain.ConsumerId;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.domain.SluttBruker;

import javax.security.auth.Subject;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;

public class InternbrukerSubjectHandler extends TestSubjectHandler {
    private Subject subject;

    public static void setVeilederIdent(String veilederident) {
        setProperty("no.nav.brukerdialog.security.context.InternbrukerSubjectHandler.veilederident", veilederident);
    }

    public static void setServicebruker(String servicebruker) {
        setProperty("no.nav.brukerdialog.security.context.InternbrukerSubjectHandler.servicebruker", servicebruker);
    }

    @Override
    public Subject getSubject() {
        Subject subject = new Subject();
        subject.getPrincipals().add(new SluttBruker(getProperty("no.nav.brukerdialog.security.context.InternbrukerSubjectHandler.veilederident", "Z999999"), IdentType.InternBruker));
        subject.getPrincipals().add(new ConsumerId(getProperty("no.nav.brukerdialog.security.context.InternbrukerSubjectHandler.servicebruker", "srvServiceBruker")));
        return subject;
    }

    @Override
    public void setSubject(Subject newSubject) {
        subject = newSubject;
    }

    @Override
    public void reset() {
        setSubject(subject);
    }
}