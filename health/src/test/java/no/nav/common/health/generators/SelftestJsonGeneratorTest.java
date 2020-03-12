package no.nav.common.health.generators;

import no.nav.common.health.domain.Selftest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SelftestJsonGeneratorTest {

    @Test
    public void smoketest() {
        assertThat(SelftestJsonGenerator.generate(Selftest.builder().build())).isEqualTo("{\"application\":null,\"version\":null,\"timestamp\":null,\"aggregateResult\":1,\"checks\":[]}");
    }

}
