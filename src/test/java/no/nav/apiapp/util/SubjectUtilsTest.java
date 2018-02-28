package no.nav.apiapp.util;

import no.nav.apiapp.security.SubjectService;
import no.nav.brukerdialog.security.context.CustomizableSubjectHandler;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.modig.core.context.StaticSubjectHandler;
import org.junit.Before;
import org.junit.Test;

import static no.nav.apiapp.util.SubjectUtils.getIdentType;
import static no.nav.apiapp.util.SubjectUtils.getUserId;
import static org.assertj.core.api.Assertions.assertThat;


public class SubjectUtilsTest {

    private static final String BRUKERDIALOG_SUBJECTHANDLER_KEY = SubjectHandler.SUBJECTHANDLER_KEY;
    private static final String MODIG_SUBJECTHANDLER_KEY = no.nav.modig.core.context.SubjectHandler.SUBJECTHANDLER_KEY;

    private SubjectService subjectService = new SubjectService();

    @Before
    public void setup() {
        System.setProperty(BRUKERDIALOG_SUBJECTHANDLER_KEY, no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler.class.getName());
        System.setProperty(MODIG_SUBJECTHANDLER_KEY, no.nav.modig.core.context.ThreadLocalSubjectHandler.class.getName());

        new no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler().setSubject(null);
        new no.nav.modig.core.context.ThreadLocalSubjectHandler().setSubject(null);

        new StaticSubjectHandler().reset();
        new CustomizableSubjectHandler().reset();
    }

    @Test
    public void getIdentType_ingenBruker_empty() {
        assertThat(getIdentType()).isEmpty();
        assertThat(subjectService.getIdentType()).isEmpty();
    }

    @Test
    public void getIdentType_internBruker() {
        System.setProperty(BRUKERDIALOG_SUBJECTHANDLER_KEY, CustomizableSubjectHandler.class.getName());
        assertThat(getIdentType()).hasValue(IdentType.InternBruker);
        assertThat(subjectService.getIdentType()).hasValue(IdentType.InternBruker);
    }

    @Test
    public void getIdentType_modigSecurityBruker() {
        System.setProperty(MODIG_SUBJECTHANDLER_KEY, StaticSubjectHandler.class.getName());
        assertThat(getIdentType()).hasValue(IdentType.EksternBruker);
        assertThat(subjectService.getIdentType()).hasValue(IdentType.EksternBruker);
    }

    @Test
    public void getUserId_ingenBruker_empty() {
        assertThat(getUserId()).isEmpty();
        assertThat(subjectService.getUserId()).isEmpty();
    }

    @Test
    public void getUserId_internBruker() {
        System.setProperty(BRUKERDIALOG_SUBJECTHANDLER_KEY, CustomizableSubjectHandler.class.getName());
        assertThat(getUserId()).hasValue("Z999999");
        assertThat(subjectService.getUserId()).hasValue("Z999999");
    }

    @Test
    public void getUserId_modigSecurityBruker() {
        System.setProperty(MODIG_SUBJECTHANDLER_KEY, StaticSubjectHandler.class.getName());
        assertThat(getUserId()).hasValue("01015245464");
        assertThat(subjectService.getUserId()).hasValue("01015245464");
    }

}