package no.nav.apiapp.selftest.impl;

import lombok.SneakyThrows;
import no.nav.common.auth.openam.sbs.OpenAmConfig;
import no.nav.dialogarena.config.fasit.FasitUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OpenAMHelsesjekkTest {

    private String validOpenAMRestUrl = FasitUtils.getOpenAmConfig().restUrl;

    @Test
    public void ok() throws Throwable {
        helsesjekk(validOpenAMRestUrl);
    }

    @Test
    public void ikkeOk() {
        assertThatThrownBy(() -> helsesjekk(validOpenAMRestUrl + "/ikke/gyldig/sti"))
                .hasMessageContaining("404");
    }

    @SneakyThrows
    private void helsesjekk(String validOpenAMRestUrl) {
        new OpenAMHelsesjekk(new OpenAmConfig(validOpenAMRestUrl)).helsesjekk();
    }

}