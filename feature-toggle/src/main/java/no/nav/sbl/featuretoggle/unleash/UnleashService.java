package no.nav.sbl.featuretoggle.unleash;

import lombok.extern.slf4j.Slf4j;
import no.finn.unleash.DefaultUnleash;
import no.finn.unleash.UnleashContext;
import no.finn.unleash.repository.FeatureToggleRepository;
import no.finn.unleash.repository.FeatureToggleResponse;
import no.finn.unleash.repository.HttpToggleFetcher;
import no.finn.unleash.repository.ToggleBackupHandlerFile;
import no.finn.unleash.strategy.Strategy;
import no.finn.unleash.util.UnleashConfig;
import no.finn.unleash.util.UnleashScheduledExecutor;
import no.finn.unleash.util.UnleashScheduledExecutorImpl;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;
import no.nav.sbl.dialogarena.types.Pingable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.finn.unleash.repository.FeatureToggleResponse.Status.CHANGED;

@Slf4j
public class UnleashService implements Pingable {

    private static final UnleashScheduledExecutor UNLEASH_SCHEDULED_EXECUTOR = new UnleashScheduledExecutorImpl();

    private final DefaultUnleash defaultUnleash;
    private final FeatureToggleRepository featureToggleRepository;
    private final Ping.PingMetadata helsesjekkMetadata;
    private final HttpToggleFetcher toggleFetcher;

    public UnleashService(UnleashServiceConfig unleashServiceConfig, Strategy... strategies) {
        this(unleashServiceConfig, Arrays.asList(strategies));
    }

    public UnleashService(UnleashServiceConfig unleashServiceConfig, List<Strategy> strategies) {
        String unleashAPI = unleashServiceConfig.unleashApiUrl;
        UnleashConfig unleashConfig = UnleashConfig.builder()
                .appName(unleashServiceConfig.applicationName)
                .unleashAPI(unleashAPI)
                .build();
        UnleashScheduledExecutor unleashScheduledExecutor = ofNullable(unleashServiceConfig.unleashScheduledExecutor).orElse(UNLEASH_SCHEDULED_EXECUTOR);

        this.toggleFetcher = new HttpToggleFetcher(unleashConfig);
        this.featureToggleRepository = new FeatureToggleRepository(
                unleashConfig,
                unleashScheduledExecutor,
                toggleFetcher,
                new ToggleBackupHandlerFile(unleashConfig)
        );
        this.helsesjekkMetadata = new Ping.PingMetadata("unleash", unleashAPI, "sjekker at feature-toggles kan hentes fra unleash server", false);
        this.defaultUnleash = new DefaultUnleash(unleashConfig, featureToggleRepository, addDefaultStrategies(strategies));
    }

    private Strategy[] addDefaultStrategies(List<Strategy> strategies) {
        List<Strategy> list = new ArrayList<>(strategies);
        list.addAll(Arrays.asList(
                new IsNotProdStrategy(),
                new ByEnvironmentStrategy()
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
            FeatureToggleResponse featureToggleResponse = this.toggleFetcher.fetchToggles();
            FeatureToggleResponse.Status status = featureToggleResponse.getStatus();
            if (status == CHANGED || status == FeatureToggleResponse.Status.NOT_CHANGED) {
                return Ping.lyktes(helsesjekkMetadata);
            } else {
                return Ping.feilet(helsesjekkMetadata, status.toString());
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return Ping.feilet(helsesjekkMetadata, e);
        }
    }

}
