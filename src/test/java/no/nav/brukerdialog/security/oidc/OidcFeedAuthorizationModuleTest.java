package no.nav.brukerdialog.security.oidc;

import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;


public class OidcFeedAuthorizationModuleTest {

    @Test
    public void skalGiTilgang() {
        System.setProperty("test.feed.brukertilgang","bruker1,bruker2");
        System.setProperty("no.nav.brukerdialog.security.context.subjectHandlerImplementationClass", InternbrukerSubjectHandler.class.getName());

        InternbrukerSubjectHandler.setVeilederIdent("bruker1");

        assertThat(new OidcFeedAuthorizationModule().isRequestAuthorized("test")).isTrue();
    }

    @Test
    public void skalIkkeGiTilgang() {
        System.setProperty("test.feed.brukertilgang","bruker1,bruker2");
        System.setProperty("no.nav.brukerdialog.security.context.subjectHandlerImplementationClass", InternbrukerSubjectHandler.class.getName());

        InternbrukerSubjectHandler.setVeilederIdent("bruker3");

        assertThat(new OidcFeedAuthorizationModule().isRequestAuthorized("test")).isFalse();
    }
}