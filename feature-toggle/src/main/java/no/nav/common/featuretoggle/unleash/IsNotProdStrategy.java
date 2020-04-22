package no.nav.common.featuretoggle.unleash;


import no.finn.unleash.strategy.Strategy;

import java.util.Map;

import static no.nav.common.utils.EnvironmentUtils.getEnvironmentName;

public class IsNotProdStrategy implements Strategy {

    @Override
    public String getName() {
        return "isNotProd";
    }

    @Override
    public boolean isEnabled(Map<String, String> map) {
        return getEnvironmentName()
                .map(e -> !isProd(e))
                .orElse(false);
    }

    private boolean isProd(String environment) {
        return "p".equals(environment) || "q0".equals(environment);
    }

}