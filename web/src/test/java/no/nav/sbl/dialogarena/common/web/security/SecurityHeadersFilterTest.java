package no.nav.sbl.dialogarena.common.web.security;

import org.junit.Test;

import static no.nav.sbl.dialogarena.common.web.security.SecurityHeadersFilter.skipAddingSecurityHeaders;
import static org.assertj.core.api.Assertions.assertThat;


public class SecurityHeadersFilterTest {

    @Test
    public void skipAddingSecurityHeaders_(){
        assertThat(skipAddingSecurityHeaders("app.nav.no")).isFalse();
        assertThat(skipAddingSecurityHeaders("localhost")).isFalse();

        assertThat(skipAddingSecurityHeaders("tjenester-q0.nav.no")).isTrue();
        assertThat(skipAddingSecurityHeaders("tjenester.nav.no")).isTrue();
    }

}