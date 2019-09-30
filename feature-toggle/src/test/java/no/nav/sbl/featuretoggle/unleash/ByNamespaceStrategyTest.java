package no.nav.sbl.featuretoggle.unleash;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static no.nav.sbl.dialogarena.test.SystemProperties.setTemporaryProperty;
import static no.nav.sbl.util.EnvironmentUtils.NAIS_CLUSTER_NAME_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.NAIS_NAMESPACE_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class ByNamespaceStrategyTest {

	private ByNamespaceStrategy byNamespaceStrategy = new ByNamespaceStrategy();

	@Test
	public void smoketest() {
		assertThat(byNamespaceStrategy.isEnabled(null)).isFalse();

		assertDisabled(null, "q0");
		assertDisabled(null, "");
		assertDisabled("q0", null);
		assertDisabled("", null);
		assertDisabled("", "");
		assertDisabled(",,,", "");
		assertDisabled("p,q1", "q6");

		assertEnabled("q0", "q0");
		assertEnabled("q6,q1,p", "q1");
	}

	private void assertDisabled(String toggleParameter, String environment) {
		assertStatus(toggleParameter, environment, false);
	}

	private void assertEnabled(String toggleParameter, String environment) {
		assertStatus(toggleParameter, environment, true);
	}

	private void assertStatus(String toggleParameter, String environment, boolean expectedState) {
		Map<String, String> parameters = new HashMap<String, String>() {{
			put("namespace", toggleParameter);
		}};
		setTemporaryProperty(NAIS_NAMESPACE_PROPERTY_NAME, environment, () -> {
					assertThat(byNamespaceStrategy.isEnabled(parameters))
							.describedAs("environment=" + environment)
							.isEqualTo(expectedState);
				}
		);
	}

}
