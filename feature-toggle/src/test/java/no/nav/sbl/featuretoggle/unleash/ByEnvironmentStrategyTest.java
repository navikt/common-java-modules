package no.nav.sbl.featuretoggle.unleash;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static no.nav.sbl.dialogarena.test.SystemProperties.setTemporaryProperty;
import static no.nav.sbl.util.EnvironmentUtils.FASIT_ENVIRONMENT_NAME_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class ByEnvironmentStrategyTest {

    private ByEnvironmentStrategy byEnvironmentStrategy = new ByEnvironmentStrategy();

    @Test
    public void smoketest() {
        assertThat(byEnvironmentStrategy.isEnabled(null)).isFalse();
        assertDisabled("p", "q0");
        assertDisabled(null, "q0");
        assertDisabled(null, "");
        assertDisabled("q0", null);
        assertDisabled("", null);
        assertDisabled("", "");
        assertDisabled(",,,", "");

        assertEnabled("q6,q1,t6", "q1");
    }

    private void assertDisabled(String environmentParameter, String environment) {
        assertStatus(environmentParameter, environment, false);
    }

    private void assertEnabled(String environmentParameter, String environment) {
        assertStatus(environmentParameter, environment, true);
    }

    private void assertStatus(String toggleParameter, String currentEnvironment, boolean expectedState) {
        Map<String, String> parameters = new HashMap<String, String>() {{
            put("miljø", toggleParameter);
        }};
        setTemporaryProperty(FASIT_ENVIRONMENT_NAME_PROPERTY_NAME, currentEnvironment, () -> {
                    assertThat(byEnvironmentStrategy.isEnabled(parameters))
                            .describedAs("environment=" + currentEnvironment)
                            .isEqualTo(expectedState);
                }
        );
    }

}