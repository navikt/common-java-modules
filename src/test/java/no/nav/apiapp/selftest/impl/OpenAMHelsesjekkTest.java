package no.nav.apiapp.selftest.impl;

import lombok.SneakyThrows;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.sbl.dialogarena.test.SystemProperties;
import org.junit.jupiter.api.Test;

import static no.nav.apiapp.config.Konfigurator.OPENAM_RESTURL;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OpenAMHelsesjekkTest {

    private String validOpenAMRestUrl = FasitUtils.getOpenAmConfig().restUrl;

    @Test
    public void ok() throws Throwable {
        SystemProperties.setTemporaryProperty(OPENAM_RESTURL, validOpenAMRestUrl, this::helsesjekk);
    }

    @Test
    public void ikkeOk() {
        assertThatThrownBy(() -> SystemProperties.setTemporaryProperty(OPENAM_RESTURL, validOpenAMRestUrl + "/ikke/gyldig/sti", this::helsesjekk))
                .hasMessageContaining("Kunne ikke verifisere om token er gyldig");
    }

    @SneakyThrows
    private void helsesjekk() {
        new OpenAMHelsesjekk().helsesjekk();
    }

}