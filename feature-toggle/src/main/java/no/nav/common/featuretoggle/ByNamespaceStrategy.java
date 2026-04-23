package no.nav.common.featuretoggle;

import io.getunleash.UnleashContext;
import io.getunleash.strategy.Strategy;
import no.nav.common.utils.EnvironmentUtils;

import java.util.Map;


public class ByNamespaceStrategy implements Strategy {

	@Override
	public String getName() {
		return "byNamespace";
	}

    @Override
    public boolean isEnabled(Map<String, String> map, UnleashContext unleashContext) {
        return ToggleChecker.isToggleEnabled("namespace", map, ByNamespaceStrategy::isNamespace);
    }

	private static boolean isNamespace(String toggleNamespace) {
		String namespace = EnvironmentUtils.getNamespace().orElse("NO_NAMESPACE");
		return namespace.equalsIgnoreCase(toggleNamespace);
	}

}
