package no.nav.apiapp.selftest.impl;

import lombok.SneakyThrows;
import no.nav.common.auth.openam.sbs.OpenAmConfig;
import no.nav.fasit.FasitUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import static no.nav.sbl.dialogarena.test.FasitAssumption.assumeFasitAccessible;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assume.assumeFalse;

public class OpenAMHelsesjekkTest {

    @BeforeClass
    public static void init() {
        assumeFasitAccessible();
        assumeFalse(FasitUtils.usingMock());
    }

    private String validOpenAMRestUrl = FasitUtils.getOpenAmConfig().restUrl;

    @Test
    public void ok() {
        helsesjekk(validOpenAMRestUrl);
    }

    @Test
    public void ikkeOk() {
        assertThatThrownBy(() -> helsesjekk(validOpenAMRestUrl + "/ikke/gyldig/sti"))
                .hasMessageContaining("404");
    }

    @SneakyThrows
    private void helsesjekk(String validOpenAMRestUrl) {
        new OpenAMHelsesjekk(OpenAmConfig.builder().restUrl(validOpenAMRestUrl).build()).helsesjekk();
    }

}