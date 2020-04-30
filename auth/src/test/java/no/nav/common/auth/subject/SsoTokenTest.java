package no.nav.common.auth.subject;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class SsoTokenTest {

    @Test
    public void toString__token_is_secret() {
        assertThat(new SsoToken(SsoToken.Type.values()[0], "secret token", Collections.emptyMap()).toString())
                .doesNotContain("secret");
    }

    @Test
    public void immutable() {
        SsoToken ssoToken = new SsoToken(SsoToken.Type.values()[0], "token", new HashMap<>());
        assertThatThrownBy(() -> ssoToken.getAttributes().put("asdf", "teasdf")).isInstanceOf(UnsupportedOperationException.class);
    }

}