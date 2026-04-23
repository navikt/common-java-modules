package no.nav.common.featuretoggle;

import io.getunleash.UnleashContext;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static no.nav.common.utils.EnvironmentUtils.NAIS_CLUSTER_NAME_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class ByClusterStrategyTest {

	private static final UnleashContext EMPTY_CONTEXT = UnleashContext.builder().build();

	private ByClusterStrategy byClusterStrategy = new ByClusterStrategy();

	@Test
	public void smoketest() {
		assertThat(byClusterStrategy.isEnabled(null, EMPTY_CONTEXT)).isFalse();

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
		Map<String, String> parameters = new HashMap<>() {{
			put("cluster", toggleParameter);
		}};

		if (cluster == null) {
			System.clearProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME);
		} else  {
			System.setProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, cluster);
		}

		assertThat(byClusterStrategy.isEnabled(parameters, EMPTY_CONTEXT))
				.describedAs("environment=" + cluster)
				.isEqualTo(expectedState);
	}

}
