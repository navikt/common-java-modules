package no.nav.sbl.dialogarena.common.abac.pep.utils;

import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.junit.Before;
import org.junit.Test;

import javax.security.auth.Subject;

import static java.lang.System.setProperty;
import static no.nav.modig.core.domain.IdentType.EksternBruker;
import static org.assertj.core.api.Assertions.assertThat;

public class SecurityUtilsTest {

    public static final String TOKEN_BODY = "bb6--bbb";
    public static final String TOKEN = "aa-aa_aa4aa." + TOKEN_BODY + ".ccccc-c_88c";

    public static final String MODIG_SUBJECTHANDLER_KEY = no.nav.modig.core.context.SubjectHandler.SUBJECTHANDLER_KEY;
    public static final String BRUKERDIALOG_SUBJECTHANDLER_KEY = no.nav.brukerdialog.security.context.SubjectHandler.SUBJECTHANDLER_KEY;

    @Before
    public void setup() {
        setProperty(MODIG_SUBJECTHANDLER_KEY, no.nav.modig.core.context.StaticSubjectHandler.class.getName());
        setProperty(BRUKERDIALOG_SUBJECTHANDLER_KEY, no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler.class.getName());
        new no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler().setSubject(null);
        new no.nav.modig.core.context.StaticSubjectHandler().setSubject(null);
    }

    @Test
    public void girRiktigTokenBodyGittHeltToken() throws PepException {
        final String token = SecurityUtils.extractOidcTokenBody(TOKEN);
        assertThat(token).isEqualTo(TOKEN_BODY);
    }

    @Test
    public void girRiktigTokenBodyGittBody() throws PepException {
        final String token = SecurityUtils.extractOidcTokenBody(TOKEN_BODY);
        assertThat(token).isEqualTo(TOKEN_BODY);
    }

    @Test
    public void getIdent_noUser_empty() throws PepException {
        assertThat(SecurityUtils.getIdent()).isEmpty();
    }

    @Test
    public void getIdent_modigUser() throws PepException {
        setProperty(MODIG_SUBJECTHANDLER_KEY, StaticSubjectHandler.class.getName());
        Subject newSubject = new Subject();
        newSubject.getPrincipals().add(new no.nav.modig.core.domain.SluttBruker("test", EksternBruker));
        new no.nav.modig.core.context.StaticSubjectHandler().setSubject(newSubject);
        assertThat(SecurityUtils.getIdent()).hasValue(StaticSubjectHandler.getSubjectHandler().getUid());
    }

    @Test
    public void getIdent_brukerdialogUser() throws PepException {
        setProperty(BRUKERDIALOG_SUBJECTHANDLER_KEY, InternbrukerSubjectHandler.class.getName());
        assertThat(SecurityUtils.getIdent()).hasValue(InternbrukerSubjectHandler.getSubjectHandler().getUid());
    }

}