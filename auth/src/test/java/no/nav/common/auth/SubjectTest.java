package no.nav.common.auth;

import no.nav.brukerdialog.security.domain.IdentType;
import org.junit.Test;

import static no.nav.common.auth.SsoToken.Type.*;
import static org.assertj.core.api.Assertions.assertThat;


public class SubjectTest {

    @Test
    public void getSsoToken() {
        Subject subject = new Subject("uid", IdentType.values()[0], new SsoToken(OIDC, "token"));
        assertThat(subject.getSsoToken(OIDC)).hasValue("token");
        assertThat(subject.getSsoToken(SAML)).isEmpty();
        assertThat(subject.getSsoToken(EKSTERN_OPENAM)).isEmpty();
    }

}