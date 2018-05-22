package no.nav.apiapp.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class JbossUtilTest {

    @Test
    public void getJbossSecurityDomain_(){
        assertThat(JbossUtil.getJbossSecurityDomain()).isEmpty();
    }

}