package no.nav.common.featuretoggle;

import no.finn.unleash.UnleashContext;
import no.finn.unleash.strategy.Strategy;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.utils.EnvironmentUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UnleashUtils {

    public final static String UNLEASH_URL_ENV_NAME = "UNLEASH_API_URL";

    public final static List<Strategy> DEFAULT_STRATEGIES = Arrays.asList(
            new ByNamespaceStrategy(),
            new ByClusterStrategy()
    );

    public static Strategy[] withDefaultStrategies(List<Strategy> additionalStrategies) {
        List<Strategy> list = new ArrayList<>(additionalStrategies);
        list.addAll(DEFAULT_STRATEGIES);
        return list.toArray(new Strategy[0]);
    }

    public static String resolveUnleashUrl() {
        return EnvironmentUtils.getRequiredProperty(UNLEASH_URL_ENV_NAME);
    }

    public static UnleashContext resolveUnleashContextFromSubject() {
        String subject = AuthContextHolderThreadLocal.instance().getSubject().orElse(null);
        String token = AuthContextHolderThreadLocal.instance().getIdTokenString().orElse(null);

        return UnleashContext.builder()
                .userId(subject)
                .sessionId(token)
                .build();
    }

}
