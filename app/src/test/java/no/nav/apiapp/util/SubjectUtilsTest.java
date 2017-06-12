package no.nav.apiapp.util;

import no.nav.apiapp.security.SubjectService;
import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.modig.core.context.StaticSubjectHandler;
import org.junit.Before;
import org.junit.Test;

import static no.nav.apiapp.util.SubjectUtils.getIdentType;
import static no.nav.apiapp.util.SubjectUtils.getUserId;
import static org.assertj.core.api.Assertions.assertThat;


public class SubjectUtilsTest {

    private SubjectService subjectService = new SubjectService();

    @Before
    public void setup() {
        System.setProperty(no.nav.brukerdialog.security.context.SubjectHandler.SUBJECTHANDLER_KEY, no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler.class.getName());
        System.setProperty(no.nav.modig.core.context.SubjectHandler.SUBJECTHANDLER_KEY, no.nav.modig.core.context.ThreadLocalSubjectHandler.class.getName());
    }

    @Test
    public void getIdentType_ingenBruker_empty() {
        assertThat(getIdentType()).isEmpty();
        assertThat(subjectService.getIdentType()).isEmpty();
    }

    @Test
    public void getIdentType_internBruker() {
        System.setProperty(no.nav.brukerdialog.security.context.SubjectHandler.SUBJECTHANDLER_KEY, InternbrukerSubjectHandler.class.getName());
        assertThat(getIdentType()).hasValue(IdentType.InternBruker);
        assertThat(subjectService.getIdentType()).hasValue(IdentType.InternBruker);
    }

    @Test
    public void getIdentType_modigSecurityBruker() {
        System.setProperty(no.nav.modig.core.context.SubjectHandler.SUBJECTHANDLER_KEY, StaticSubjectHandler.class.getName());
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
        System.setProperty(no.nav.brukerdialog.security.context.SubjectHandler.SUBJECTHANDLER_KEY, InternbrukerSubjectHandler.class.getName());
        assertThat(getUserId()).hasValue("Z999999");
        assertThat(subjectService.getUserId()).hasValue("Z999999");
    }

    @Test
    public void getUserId_modigSecurityBruker() {
        System.setProperty(no.nav.modig.core.context.SubjectHandler.SUBJECTHANDLER_KEY, StaticSubjectHandler.class.getName());
        assertThat(getUserId()).hasValue("01015245464");
        assertThat(subjectService.getUserId()).hasValue("01015245464");
    }

}