package no.nav.sbl.featuretoggle.unleash;


import no.nav.sbl.dialogarena.test.SystemProperties;
import org.junit.Test;

import java.util.Arrays;

import static no.nav.util.sbl.EnvironmentUtils.FASIT_ENVIRONMENT_NAME_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;


public class IsNotProdStrategyTest {

    private IsNotProdStrategy isNotProdStrategy = new IsNotProdStrategy();

    @Test
    public void smoketest() {
        assertThat(isNotProdStrategy.isEnabled(null)).isFalse();
        assertDisabled("p", "q0");
        assertEnabled("q1", "q6", "t6");
    }

    private void assertDisabled(String... environments) {
        assertStatus(false, environments);
    }

    private void assertEnabled(String... environments) {
        assertStatus(true, environments);
    }

    private void assertStatus(boolean expected, String[] environments) {
        Arrays.stream(environments).forEach(environment -> SystemProperties.setTemporaryProperty(FASIT_ENVIRONMENT_NAME_PROPERTY_NAME, environment, () ->
                assertThat(isNotProdStrategy.isEnabled(null))
                        .describedAs("environment=" + environment)
                        .isEqualTo(expected)
        ));
    }

}
