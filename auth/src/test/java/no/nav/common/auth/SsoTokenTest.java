package no.nav.common.auth;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class SsoTokenTest {

    @Test
    public void toString__token_is_secret() {
        assertThat(new SsoToken(SsoToken.Type.values()[0], "secret token").toString())
                .doesNotContain("secret");
    }

}