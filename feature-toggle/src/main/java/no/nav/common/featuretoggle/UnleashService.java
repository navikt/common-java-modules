package no.nav.common.featuretoggle;

import lombok.extern.slf4j.Slf4j;
import no.finn.unleash.DefaultUnleash;
import no.finn.unleash.UnleashContext;
import no.finn.unleash.UnleashException;
import no.finn.unleash.event.UnleashSubscriber;
import no.finn.unleash.repository.FeatureToggleResponse;
import no.finn.unleash.strategy.Strategy;
import no.finn.unleash.util.UnleashConfig;
import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.Subject;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.selftest.HealthCheckMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static no.finn.unleash.repository.FeatureToggleResponse.Status.CHANGED;

@Slf4j
public class UnleashService implements HealthCheck, UnleashSubscriber {

    private final DefaultUnleash defaultUnleash;
    private final UnleashServiceConfig unleashServiceConfig;

    private FeatureToggleResponse.Status lastTogglesFetchedStatus;

    public UnleashService(UnleashServiceConfig unleashServiceConfig, Strategy... strategies) {
        this(unleashServiceConfig, Arrays.asList(strategies));
    }

    public UnleashService(UnleashServiceConfig unleashServiceConfig, List<Strategy> strategies) {
        String unleashAPI = unleashServiceConfig.unleashApiUrl;

        UnleashConfig.Builder builder = ofNullable(unleashServiceConfig.unleashBuilderFactory)
                .map(Supplier::get)
                .orElseGet(UnleashConfig::builder);

        UnleashConfig unleashConfig = builder
                .appName(unleashServiceConfig.applicationName)
                .unleashAPI(unleashAPI)
                .subscriber(this)
                .synchronousFetchOnInitialisation(true)
                .build();

        this.defaultUnleash = new DefaultUnleash(unleashConfig, addDefaultStrategies(strategies));
        this.unleashServiceConfig = unleashServiceConfig;
    }

    private Strategy[] addDefaultStrategies(List<Strategy> strategies) {
        List<Strategy> list = new ArrayList<>(strategies);
        list.addAll(Arrays.asList(
                new ByNamespaceStrategy(),
                new ByClusterStrategy()
        ));
        return list.toArray(new Strategy[0]);
    }

    public boolean isEnabled(String toggleName) {
        return isEnabled(toggleName, resolveUnleashContextFromSubject());
    }

    public boolean isEnabled(String toggleName, UnleashContext unleashContext) {
        return defaultUnleash.isEnabled(toggleName, unleashContext);
    }

    public static UnleashContext resolveUnleashContextFromSubject() {
        Optional<Subject> subject = SubjectHandler.getSubject();
        return UnleashContext.builder()
                .userId(subject.map(Subject::getUid).orElse(null))
                .sessionId(subject.map(Subject::getSsoToken).map(SsoToken::getToken).orElse(null))
                .build();
    }

    @Override
    public void togglesFetched(FeatureToggleResponse toggleResponse) {
        this.lastTogglesFetchedStatus = toggleResponse.getStatus();
    }

    @Override
    public void onError(UnleashException unleashException) {
        log.warn(unleashException.getMessage(), unleashException);
    }

    @Override
    public HealthCheckResult checkHealth() {
        try {
            if (lastTogglesFetchedStatus == CHANGED || lastTogglesFetchedStatus == FeatureToggleResponse.Status.NOT_CHANGED) {
                return HealthCheckResult.healthy();
            } else {
                return HealthCheckResult.unhealthy(lastTogglesFetchedStatus.toString());
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return HealthCheckResult.unhealthy(e.getMessage(), e);
        }
    }

    @Override
    public HealthCheckMetadata healthCheckMetadata() {
        return new HealthCheckMetadata("Unleash", unleashServiceConfig.erKritisk, unleashServiceConfig.unleashApiUrl);
    }
}
