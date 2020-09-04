package no.nav.common.featuretoggle;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static no.nav.common.utils.EnvironmentUtils.NAIS_NAMESPACE_PROPERTY_NAME;
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

	private void assertDisabled(String toggleParameter, String namespace) {
		assertStatus(toggleParameter, namespace, false);
	}

	private void assertEnabled(String toggleParameter, String namespace) {
		assertStatus(toggleParameter, namespace, true);
	}

	private void assertStatus(String toggleParameter, String namespace, boolean expectedState) {
		Map<String, String> parameters = new HashMap<>() {{
			put("namespace", toggleParameter);
		}};

		if (namespace == null) {
			System.clearProperty(NAIS_NAMESPACE_PROPERTY_NAME);
		} else  {
			System.setProperty(NAIS_NAMESPACE_PROPERTY_NAME, namespace);
		}

		assertThat(byNamespaceStrategy.isEnabled(parameters))
				.describedAs("environment=" + namespace)
				.isEqualTo(expectedState);
	}

}
