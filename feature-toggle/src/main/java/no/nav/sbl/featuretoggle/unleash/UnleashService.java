package no.nav.sbl.featuretoggle.unleash;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.finn.unleash.DefaultUnleash;
import no.finn.unleash.UnleashContext;
import no.finn.unleash.UnleashException;
import no.finn.unleash.event.ToggleEvaluated;
import no.finn.unleash.event.UnleashEvent;
import no.finn.unleash.event.UnleashSubscriber;
import no.finn.unleash.repository.FeatureToggleResponse;
import no.finn.unleash.strategy.Strategy;
import no.finn.unleash.util.UnleashConfig;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;
import no.nav.metrics.MetricsFactory;
import no.nav.sbl.dialogarena.types.Pingable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static no.finn.unleash.repository.FeatureToggleResponse.Status.CHANGED;

@Slf4j
public class UnleashService implements Pingable, UnleashSubscriber {

    private final MeterRegistry meterRegistry = MetricsFactory.getMeterRegistry();

    private final DefaultUnleash defaultUnleash;
    private final Ping.PingMetadata helsesjekkMetadata;

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

        this.helsesjekkMetadata = new Ping.PingMetadata("unleash", unleashAPI, "sjekker at feature-toggles kan hentes fra unleash server", false);
        this.defaultUnleash = new DefaultUnleash(unleashConfig, addDefaultStrategies(strategies));
    }

    private Strategy[] addDefaultStrategies(List<Strategy> strategies) {
        List<Strategy> list = new ArrayList<>(strategies);
        list.addAll(Arrays.asList(
                new IsNotProdStrategy(),
                new ByEnvironmentStrategy(),
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
    public Ping ping() {
        try {
            if (lastTogglesFetchedStatus == CHANGED || lastTogglesFetchedStatus == FeatureToggleResponse.Status.NOT_CHANGED) {
                return Ping.lyktes(helsesjekkMetadata);
            } else {
                return Ping.feilet(helsesjekkMetadata, lastTogglesFetchedStatus.toString());
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return Ping.feilet(helsesjekkMetadata, e);
        }
    }

    @Override
    public void toggleEvaluated(ToggleEvaluated toggleEvaluated) {
        meterRegistry.counter("unleash_toggle",
                "name",
                toggleEvaluated.getToggleName(),
                "enabled",
                Boolean.toString(toggleEvaluated.isEnabled())
        ).increment();
    }

    @Override
    public void togglesFetched(FeatureToggleResponse toggleResponse) {
        this.lastTogglesFetchedStatus = toggleResponse.getStatus();
        meterRegistry.counter("unleash_fetch",
                "status",
                toggleResponse.getStatus().name(),
                "httpStatus",
                Integer.toString(toggleResponse.getHttpStatusCode())
        ).increment();
    }

    @Override
    public void on(UnleashEvent unleashEvent) {
        meterRegistry.counter("unleash_event",
                "type",
                unleashEvent.getClass().getSimpleName()
        ).increment();
    }

    @Override
    public void onError(UnleashException unleashException) {
        log.warn(unleashException.getMessage(), unleashException);
    }

}
