package no.nav.common.featuretoggle;


import no.finn.unleash.strategy.Strategy;
import no.nav.common.utils.EnvironmentUtils;

import java.util.Map;


public class ByNamespaceStrategy implements Strategy {

	@Override
	public String getName() {
		return "byNamespace";
	}

	@Override
	public boolean isEnabled(Map<String, String> parameters) {
		return ToggleChecker.isToggleEnabled("namespace", parameters, ByNamespaceStrategy::isNamespace);
	}

	private static boolean isNamespace(String toggleNamespace) {
		String namespace = EnvironmentUtils.getNamespace().orElse("NO_NAMESPACE");
		return namespace.equalsIgnoreCase(toggleNamespace);
	}

}
