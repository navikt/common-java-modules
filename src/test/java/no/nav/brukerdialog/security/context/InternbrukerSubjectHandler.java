package no.nav.brukerdialog.security.context;

import no.nav.brukerdialog.security.domain.*;

import javax.security.auth.Subject;

public class InternbrukerSubjectHandler extends TestSubjectHandler {
    private Subject subject;
    private static OidcCredential oidcCredential;
    private static String veilederIdent = "Z999999";
    private static String servicebruker = "srvServicebruker";

    public static void setVeilederIdent(String ident) {
        veilederIdent = ident;
    }

    public static void setServicebruker(String bruker) {
        servicebruker = bruker;
    }
    public static void setOidcCredential(OidcCredential credential) {
        oidcCredential = credential;
    }

    @Override
    public Subject getSubject() {
        Subject subject = new Subject();

        subject.getPrincipals().add(new SluttBruker(veilederIdent, IdentType.InternBruker));
        subject.getPrincipals().add(new ConsumerId(servicebruker));
        if (oidcCredential != null) {
            subject.getPublicCredentials().add(new OidcCredential(oidcCredential.getToken()));
        }

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