package no.nav.brukerdialog.security.oidc;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class SystemUserTokenProviderTest {

    @Test
    public void skalSetteSammenTilKorrektAuthUrl() {
        String openAmHost = "https://isso-t.adeo.no/isso/oauth2";
        String authenticateUri = "json/authenticate?authIndexType=service&authIndexValue=adminconsoleservice";
        String korrektStreng = "https://isso-t.adeo.no/isso/json/authenticate?authIndexType=service&authIndexValue=adminconsoleservice";

        assertThat(SystemUserTokenProvider.konstruerFullstendingAuthUri(openAmHost, authenticateUri), is(korrektStreng));

    }

}