package no.nav.brukerdialog.security.oidc;

import no.nav.brukerdialog.security.context.CustomizableSubjectHandler;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;


public class OidcFeedAuthorizationModuleTest {

    @Test
    public void skalGiTilgang() {
        System.setProperty("test.feed.brukertilgang","bruker1,bruker2");
        System.setProperty("no.nav.brukerdialog.security.context.subjectHandlerImplementationClass", CustomizableSubjectHandler.class.getName());

        CustomizableSubjectHandler.setUid("bruker1");

        assertThat(new OidcFeedAuthorizationModule().isRequestAuthorized("test")).isTrue();
    }

    @Test
    public void skalIkkeGiTilgang() {
        System.setProperty("test.feed.brukertilgang","bruker1,bruker2");
        System.setProperty("no.nav.brukerdialog.security.context.subjectHandlerImplementationClass", CustomizableSubjectHandler.class.getName());

        CustomizableSubjectHandler.setUid("bruker3");

        assertThat(new OidcFeedAuthorizationModule().isRequestAuthorized("test")).isFalse();
    }
}