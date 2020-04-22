package no.nav.common.abac.utils;

import no.nav.common.abac.exception.PepException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SecurityUtilsTest {

    public static final String TOKEN_BODY = "bb6--bbb";
    public static final String TOKEN = "aa-aa_aa4aa." + TOKEN_BODY + ".ccccc-c_88c";


    @Test
    public void girRiktigTokenBodyGittHeltToken() throws PepException {
        final String token = SecurityUtils.extractOidcTokenBody(TOKEN);
        assertThat(token).isEqualTo(TOKEN_BODY);
    }

    @Test
    public void girRiktigTokenBodyGittBody() throws PepException {
        final String token = SecurityUtils.extractOidcTokenBody(TOKEN_BODY);
        assertThat(token).isEqualTo(TOKEN_BODY);
    }

}