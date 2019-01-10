package no.nav.apiapp.security;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static no.nav.common.auth.SsoToken.eksternOpenAM;
import static no.nav.common.auth.SsoToken.oidcToken;
import static no.nav.common.auth.SsoToken.saml;
import static org.assertj.core.api.Assertions.assertThat;

public class SecurityLevelAuthorizationModuleTest {

    private SecurityLevelAuthorizationModule securityLevelAuthorizationModule = new SecurityLevelAuthorizationModule(3);

    @Test
    public void authorized__no_subject() {
        assertThat(securityLevelAuthorizationModule.authorized(null, null)).isFalse();
    }

    @Test
    public void authorized__unrelated_subject() {
        Subject subject = createSubject(saml("test-saml", Collections.emptyMap()));
        assertThat(securityLevelAuthorizationModule.authorized(subject, null)).isFalse();
    }

    @Test
    public void authorized__too_low_level() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("acr", "Level1");
        SsoToken ssoToken = oidcToken("oidc-token", attributes);
        Subject subject1 = createSubject(ssoToken);
        assertThat(securityLevelAuthorizationModule.authorized(subject1, null)).isFalse();
    }

    @Test
    public void authorized__oidc() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("acr", "Level3");
        Subject subject = createSubject(oidcToken("oidc-token", attributes));
        assertThat(securityLevelAuthorizationModule.authorized(subject, null)).isTrue();
    }

    @Test
    public void authorized__openam() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("SecurityLevel", "3");
        Subject subject = createSubject(eksternOpenAM("openam-token", attributes));
        assertThat(securityLevelAuthorizationModule.authorized(subject, null)).isTrue();
    }

    private Subject createSubject(SsoToken ssoToken) {
        return new Subject("test-ident", IdentType.EksternBruker, ssoToken);
    }

}