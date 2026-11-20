package no.nav.common.featuretoggle;

import io.getunleash.UnleashContext;
import io.getunleash.strategy.Strategy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.types.identer.Id;
import no.nav.common.utils.EnvironmentUtils;

public class UnleashUtils {

    public static final String UNLEASH_URL_ENV_NAME = "UNLEASH_API_URL";

    public static final List<Strategy> DEFAULT_STRATEGIES =
            Arrays.asList(new ByNamespaceStrategy(), new ByClusterStrategy());

    public static Strategy[] withDefaultStrategies(List<Strategy> additionalStrategies) {
        List<Strategy> list = new ArrayList<>(additionalStrategies);
        list.addAll(DEFAULT_STRATEGIES);
        return list.toArray(new Strategy[0]);
    }

    public static String resolveUnleashUrl() {
        return EnvironmentUtils.getRequiredProperty(UNLEASH_URL_ENV_NAME);
    }

    public static UnleashContext resolveUnleashContextFromSubject() {
        String subject;
        if (AuthContextHolderThreadLocal.instance().erInternBruker()) {
            subject = AuthContextHolderThreadLocal.instance()
                    .getNavIdent()
                    .map(Id::toString)
                    .orElse(null);
        } else {
            subject = AuthContextHolderThreadLocal.instance().getSubject().orElse(null);
        }
        String token =
                AuthContextHolderThreadLocal.instance().getIdTokenString().orElse(null);

        return UnleashContext.builder().userId(subject).sessionId(token).build();
    }
}
