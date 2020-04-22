package no.nav.common.featuretoggle.unleash;


import no.finn.unleash.strategy.Strategy;
import no.nav.common.utils.EnvironmentUtils;

import java.util.Map;


public class ByEnvironmentStrategy implements Strategy {

    @Override
    public String getName() {
        return "byEnvironment";
    }

    @Override
    public boolean isEnabled(Map<String, String> parameters) {
        return ToggleChecker.isToggleEnabled("miljø", parameters, ByEnvironmentStrategy::isEnvironment);
    }

    private static boolean isEnvironment(String toggleEnvironment) {
        String environment = EnvironmentUtils.getEnvironmentName().orElse("NO_ENVIRONMENT");
        return environment.equalsIgnoreCase(toggleEnvironment);
    }

}
