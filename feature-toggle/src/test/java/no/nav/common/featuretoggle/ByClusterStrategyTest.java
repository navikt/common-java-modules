package no.nav.common.featuretoggle;

import static no.nav.common.utils.EnvironmentUtils.NAIS_CLUSTER_NAME_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import io.getunleash.UnleashContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ByClusterStrategyTest {

    private static final UnleashContext EMPTY_CONTEXT = UnleashContext.builder().build();

    private final ByClusterStrategy byClusterStrategy = new ByClusterStrategy();
    private Properties originalProperties;

    @Before
    public void setUp() {
        originalProperties = (Properties) System.getProperties().clone();
    }

    @After
    public void tearDown() {
        System.setProperties(originalProperties);
    }

    @Test
    @SuppressWarnings("DataFlowIssue") // Tester defensiv null-input utenfor SDK-kontrakten.
    public void skal_returnere_false_naar_parameterkart_er_null() {
        assertThat(byClusterStrategy.isEnabled(null, EMPTY_CONTEXT)).isFalse();
    }

    @Test
    public void smokeTest() {
        assertDisabled(null, "preprod-fss");
        assertDisabled(null, "");
        assertDisabled("preprod-fss", null);
        assertDisabled("", null);
        assertDisabled("", "");
        assertDisabled(",,,", "");
        assertDisabled("preprod-fss,dev-sbs", "prod-fss");

        assertEnabled("preprod-fss", "preprod-fss");
        assertEnabled("preprod-fss,dev-sbs", "dev-sbs");
    }

    private void assertDisabled(String toggleParameter, String cluster) {
        assertStatus(toggleParameter, cluster, false);
    }

    private void assertEnabled(String toggleParameter, String cluster) {
        assertStatus(toggleParameter, cluster, true);
    }

    private void assertStatus(String toggleParameter, String cluster, boolean expectedState) {
        Map<String, String> parameters = new HashMap<>() {
            {
                put("cluster", toggleParameter);
            }
        };

        if (cluster == null) {
            System.clearProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME);
        } else {
            System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, cluster);
        }

        assertThat(byClusterStrategy.isEnabled(parameters, EMPTY_CONTEXT))
                .describedAs("environment=" + cluster)
                .isEqualTo(expectedState);
    }
}
