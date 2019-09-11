package no.nav.sbl.featuretoggle.unleash;


import no.finn.unleash.strategy.Strategy;
import no.nav.util.sbl.EnvironmentUtils;

import java.util.Arrays;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class ByEnvironmentStrategy implements Strategy {

    @Override
    public String getName() {
        return "byEnvironment";
    }

    @Override
    public boolean isEnabled(Map<String, String> map) {
        return ofNullable(map)
                .map(m -> m.get("miljø")).map(s -> s.split(","))
                .map(Arrays::stream)
                .map(s -> s.anyMatch(this::isEnvironment))
                .orElse(false);
    }

    private boolean isEnvironment(String environment) {
        return EnvironmentUtils.getEnvironmentName().map(environment::equals).orElse(false);
    }

}
