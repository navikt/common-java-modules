package no.nav.common.featuretoggle.unleash;


import no.finn.unleash.strategy.Strategy;
import no.nav.common.utils.EnvironmentUtils;

import java.util.Map;


public class ByClusterStrategy implements Strategy {

	@Override
	public String getName() {
		return "byCluster";
	}

	@Override
	public boolean isEnabled(Map<String, String> parameters) {
		return ToggleChecker.isToggleEnabled("cluster", parameters, ByClusterStrategy::isCluster);
	}

	private static boolean isCluster(String toggleClusterName) {
		String clusterName = EnvironmentUtils.getClusterName().orElse("NO_CLUSTER_NAME");
		return clusterName.equalsIgnoreCase(toggleClusterName);
	}

}
