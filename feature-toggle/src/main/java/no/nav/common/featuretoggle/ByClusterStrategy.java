package no.nav.common.featuretoggle;

import io.getunleash.UnleashContext;
import io.getunleash.strategy.Strategy;
import no.nav.common.utils.EnvironmentUtils;

import java.util.Map;


public class ByClusterStrategy implements Strategy {

	@Override
	public String getName() {
		return "byCluster";
	}

	@Override
	public boolean isEnabled(Map<String, String> map, UnleashContext unleashContext) {
		return ToggleChecker.isToggleEnabled("cluster", map, ByClusterStrategy::isCluster);
	}

	private static boolean isCluster(String toggleClusterName) {
		String clusterName = EnvironmentUtils.getClusterName().orElse("NO_CLUSTER_NAME");
		return clusterName.equalsIgnoreCase(toggleClusterName);
	}

}
