package no.nav.common.auth;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;


public class SecurityLevelTest {

    @Test
    public void should_not_support_open_AM() {
        SsoToken ssoTokenOpenAM = SsoToken.eksternOpenAM("Token", new HashMap<>());

        SecurityLevel securityLevel = SecurityLevel.getOidcSecurityLevel(ssoTokenOpenAM);

        assertThat(securityLevel).isEqualTo(SecurityLevel.Ukjent);
    }

    @Test
    public void should_not_support_saml() {
        SsoToken ssoTokenSaml = SsoToken.saml("Token", new HashMap<>());

        SecurityLevel securityLevel = SecurityLevel.getOidcSecurityLevel(ssoTokenSaml);

        assertThat(securityLevel).isEqualTo(SecurityLevel.Ukjent);
    }

    @Test
    public void should_return_ukjent_when_acr_prop_is_undefined() {
        SsoToken ssoTokenOidc = SsoToken.oidcToken("Token", new HashMap<>());

        SecurityLevel securityLevel = SecurityLevel.getOidcSecurityLevel(ssoTokenOidc);

        assertThat(securityLevel).isEqualTo(SecurityLevel.Ukjent);
    }

    @Test
    public void should_return_correct_security_level() {
        SsoToken ssoToken = SsoToken.oidcToken("Token", Collections.singletonMap("acr", "Level3"));

        SecurityLevel securityLevel = SecurityLevel.getOidcSecurityLevel(ssoToken);

        assertThat(securityLevel).isEqualTo(SecurityLevel.Level3);
    }
}
