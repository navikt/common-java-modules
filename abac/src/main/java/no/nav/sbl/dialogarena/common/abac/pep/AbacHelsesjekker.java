package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import org.springframework.stereotype.Component;

@Component
public class AbacHelsesjekker implements HealthCheck {

    private final PepImpl pepClient;

    public AbacHelsesjekker(PepImpl pepClient) {
        this.pepClient = pepClient;
    }

    @Override
    public HealthCheckResult checkHealth() {
        try {
            pepClient.ping();
            return HealthCheckResult.healthy();
        } catch (Throwable e) {
            return HealthCheckResult.unhealthy(e);
        }
    }

}
