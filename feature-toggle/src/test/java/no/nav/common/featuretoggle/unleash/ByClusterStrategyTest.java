package no.nav.common.featuretoggle.unleash;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static no.nav.common.test.SystemProperties.setTemporaryProperty;
import static no.nav.common.utils.EnvironmentUtils.NAIS_CLUSTER_NAME_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class ByClusterStrategyTest {

	private ByClusterStrategy byClusterStrategy = new ByClusterStrategy();

	@Test
	public void smoketest() {
		assertThat(byClusterStrategy.isEnabled(null)).isFalse();

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

	private void assertDisabled(String toggleParameter, String environment) {
		assertStatus(toggleParameter, environment, false);
	}

	private void assertEnabled(String toggleParameter, String environment) {
		assertStatus(toggleParameter, environment, true);
	}

	private void assertStatus(String toggleParameter, String environment, boolean expectedState) {
		Map<String, String> parameters = new HashMap<String, String>() {{
			put("cluster", toggleParameter);
		}};
		setTemporaryProperty(NAIS_CLUSTER_NAME_PROPERTY_NAME, environment, () -> {
					assertThat(byClusterStrategy.isEnabled(parameters))
							.describedAs("environment=" + environment)
							.isEqualTo(expectedState);
				}
		);
	}

}
