package no.nav.common.auth;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;


public class JWTSecurityLevelTest {


    @Test
    public void should_not_support_open_AM() {
        SsoToken ssoTokenOpenAM = SsoToken.eksternOpenAM("Token", new HashMap<>());

        JWTSecurityLevel jwtSecurityLevelOpenAM = new JWTSecurityLevel(ssoTokenOpenAM);

        assertThat(jwtSecurityLevelOpenAM.getSecurityLevel()).isEqualTo(SecurityLevel.Ukjent);
    }

    @Test
    public void should_not_support_saml() {
        SsoToken ssoTokenSaml = SsoToken.saml("Token", new HashMap<>());

        JWTSecurityLevel jwtSecurityLevelSaml = new JWTSecurityLevel(ssoTokenSaml);

        assertThat(jwtSecurityLevelSaml.getSecurityLevel()).isEqualTo(SecurityLevel.Ukjent);
    }

    @Test
    public void should_return_ukjent_when_acr_prop_is_undefined() {
        SsoToken ssoTokenSaml = SsoToken.oidcToken("Token", new HashMap<>());
        JWTSecurityLevel jwtSecurityLevelSaml = new JWTSecurityLevel(ssoTokenSaml);

        assertThat(jwtSecurityLevelSaml.getSecurityLevel()).isEqualTo(SecurityLevel.Ukjent);
    }

    @Test
    public void should_return_correct_security_level() {
        SsoToken ssoToken = SsoToken.oidcToken("Token", Collections.singletonMap("acr", "Level3"));
        JWTSecurityLevel jwtSecurityLevel = new JWTSecurityLevel(ssoToken);

        assertThat(jwtSecurityLevel.getSecurityLevel()).isEqualTo(SecurityLevel.Level3);
    }
}
