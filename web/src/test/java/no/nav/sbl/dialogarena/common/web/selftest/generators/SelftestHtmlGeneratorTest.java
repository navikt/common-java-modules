package no.nav.sbl.dialogarena.common.web.selftest.generators;

import no.nav.sbl.dialogarena.common.web.selftest.domain.Selftest;
import no.nav.sbl.dialogarena.common.web.selftest.domain.SelftestResult;
import org.junit.Test;

import java.io.IOException;

import static java.util.Arrays.asList;
import static no.nav.sbl.dialogarena.common.web.selftest.generators.SelftestHtmlGenerator.generate;
import static org.assertj.core.api.Assertions.assertThat;

public class SelftestHtmlGeneratorTest {

    @Test
    public void generate_fungerer_med_minimal_input() throws IOException {
        assertThat(generate(null, null)).isNotEmpty();
        assertThat(generate(newSelftest(SelftestResult.builder().build()), null)).isNotEmpty();
        assertThat(generate(newSelftest(), null)).isNotEmpty();
        assertThat(generate(null, "test")).isNotEmpty();
        assertThat(generate(newSelftest(), "test")).isNotEmpty();
    }

    private Selftest newSelftest(SelftestResult... selftestResults) {
        return Selftest.builder()
                .checks(asList(selftestResults))
                .build();
    }

}