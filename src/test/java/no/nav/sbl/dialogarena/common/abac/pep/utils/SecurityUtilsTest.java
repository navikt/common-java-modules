package no.nav.sbl.dialogarena.common.abac.pep.utils;

import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.brukerdialog.security.context.TestSubjectHandler;
import no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler;
import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.junit.Before;
import org.junit.Test;

import static java.lang.System.setProperty;
import static org.assertj.core.api.Assertions.assertThat;

public class SecurityUtilsTest {

    public static final String TOKEN_BODY = "bb6--bbb";
    public static final String TOKEN = "aa-aa_aa4aa." + TOKEN_BODY + ".ccccc-c_88c";

    public static final String MODIG_SUBJECTHANDLER_KEY = no.nav.modig.core.context.SubjectHandler.SUBJECTHANDLER_KEY;
    public static final String BRUKERDIALOG_SUBJECTHANDLER_KEY = no.nav.brukerdialog.security.context.SubjectHandler.SUBJECTHANDLER_KEY;

    @Before
    public void setup() {
        setProperty(MODIG_SUBJECTHANDLER_KEY, no.nav.modig.core.context.ThreadLocalSubjectHandler.class.getName());
        setProperty(BRUKERDIALOG_SUBJECTHANDLER_KEY, no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler.class.getName());
        new no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler().setSubject(null);
        new no.nav.modig.core.context.ThreadLocalSubjectHandler().setSubject(null);
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
        assertThat(SecurityUtils.getIdent()).hasValue(StaticSubjectHandler.getSubjectHandler().getUid());
    }

    @Test
    public void getIdent_brukerdialogUser() throws PepException {
        setProperty(BRUKERDIALOG_SUBJECTHANDLER_KEY, InternbrukerSubjectHandler.class.getName());
        assertThat(SecurityUtils.getIdent()).hasValue(InternbrukerSubjectHandler.getSubjectHandler().getUid());
    }

}