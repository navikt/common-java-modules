package no.nav.common.featuretoggle;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import io.getunleash.UnleashException;
import io.getunleash.event.ClientFeaturesResponse;
import io.getunleash.event.UnleashSubscriber;
import io.getunleash.strategy.Strategy;
import io.getunleash.util.UnleashConfig;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;

import java.util.Collections;
import java.util.List;

import static no.nav.common.featuretoggle.UnleashUtils.resolveUnleashContextFromSubject;
import static no.nav.common.featuretoggle.UnleashUtils.withDefaultStrategies;

@Slf4j
public class UnleashClientImpl implements HealthCheck, UnleashSubscriber, UnleashClient {

    private final Unleash defaultUnleash;

    private ClientFeaturesResponse.Status lastTogglesFetchedStatus;

    public UnleashClientImpl(Unleash defaultUnleash) {
        this.defaultUnleash = defaultUnleash;
    }

    public UnleashClientImpl(String unleashUrl, String applicationName) {
        this(unleashUrl, applicationName, Collections.emptyList());
    }

    public UnleashClientImpl(String unleashUrl, String applicationName, List<Strategy> additionalStrategies) {
        UnleashConfig unleashConfig = UnleashConfig.builder()
                .appName(applicationName)
                .unleashAPI(unleashUrl)
                .subscriber(this)
                .synchronousFetchOnInitialisation(true)
                .build();

        this.defaultUnleash = new DefaultUnleash(unleashConfig, withDefaultStrategies(additionalStrategies));
    }

    UnleashClientImpl(UnleashConfig.Builder builder, List<Strategy> additionalStrategies) {
        UnleashConfig config = builder
                .subscriber(this)
                .build();

        this.defaultUnleash = new DefaultUnleash(config, withDefaultStrategies(additionalStrategies));
    }

    @Override
    public boolean isEnabled(String toggleName) {
        return isEnabled(toggleName, resolveUnleashContextFromSubject());
    }

    @Override
    public boolean isEnabled(String toggleName, UnleashContext unleashContext) {
        return defaultUnleash.isEnabled(toggleName, unleashContext);
    }

    @Override
    public void togglesFetched(ClientFeaturesResponse toggleResponse) {
        this.lastTogglesFetchedStatus = toggleResponse.getStatus();
    }

    @Override
    public void onError(UnleashException unleashException) {
        log.warn(unleashException.getMessage(), unleashException);
    }

    @Override
    public HealthCheckResult checkHealth() {
        try {
            if (lastTogglesFetchedStatus == ClientFeaturesResponse.Status.CHANGED || lastTogglesFetchedStatus == ClientFeaturesResponse.Status.NOT_CHANGED) {
                return HealthCheckResult.healthy();
            } else {
                return HealthCheckResult.unhealthy(
                        lastTogglesFetchedStatus == null ? "no status yet" : lastTogglesFetchedStatus.toString());
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return HealthCheckResult.unhealthy(e.getMessage(), e);
        }
    }
}
